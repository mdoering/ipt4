/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.web.account;

import org.gbif.ipt.web.support.action.PostActionPage;
import org.gbif.ipt.web.support.action.PageBean;
import org.gbif.ipt.web.support.page.PageContext;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.Constants;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.i18n.SimpleTextProvider;

import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.Serial;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gbif.ipt.web.security.LocalPaths;
import org.gbif.ipt.web.support.action.StrutsParameter;

import lombok.Getter;

/**
 * Action handling login/logout only. Login can happen both from small login box on every page, or dedicated login
 * page that provides the IPT administrator contact in case of problems or to create a new account.
 */
@PageBean
public class LoginPage extends PostActionPage {

  private static final Logger LOG = LogManager.getLogger(LoginPage.class);

  @Serial
  private static final long serialVersionUID = -863287752175768744L;

  private final UserAccountManager userManager;

  @Getter
  private String redirectUrl;
  @Getter
  private String email;
  @Getter
  private String password;
  @Getter
  private String adminEmail;

  @Inject
  public LoginPage(
      PageContext ctx,
      UserAccountManager userManager) {
    super(ctx);
    this.userManager = userManager;
  }

  @Override
  public void prepare() {
    super.prepare();

    // If the user clicked "login" from a public page, no interceptor stored the original URL.
    // Capture it from the HTTP Referer header (only if it points to our own base URL).
    if (session.get(Constants.SESSION_REFERER) == null && req != null) {
      String refererHeader = req.getHeader("Referer");
      String base = getBase();

      if (StringUtils.isNotBlank(refererHeader) && StringUtils.isNotBlank(base) && refererHeader.startsWith(base)) {
        String relative = refererHeader.substring(base.length());
        // normalize to something like "/resource?r=...", never "//host"
        relative = LocalPaths.of(relative);

        // avoid redirecting back to the login page itself
        if (!(relative.endsWith("login.do") || relative.endsWith("login"))) {
          LOG.debug("Storing referer from header into session: {}", relative);
          session.put(Constants.SESSION_REFERER, relative);
        }
      }
    }

    adminEmail = userManager.getDefaultAdminEmail();
    if (StringUtils.isBlank(adminEmail)) {
      List<User> users = userManager.list(User.Role.Admin);

      if (!users.isEmpty()) {
        adminEmail = users.get(0).getEmail();
      } else {
        LOG.error("Failed to load the default admin email");
      }
    }
  }

  /**
   * Shows the login form. Spring Security checks the posted credentials (see SecurityConfig and
   * LoginSuccessHandler); a failed attempt comes back here with {@code error}.
   */
  public String login() {
    // user already logged in, return
    if (getCurrentUser() != null) {
      setRedirectUrl();
      return SUCCESS;
    }
    if (req.getParameter("error") != null) {
      addActionError(getText("admin.user.wrong.email.password.combination"));
    }
    // IPT 3 wrote the rejected password back into the form
    password = null;
    return INPUT;
  }

  public String logout() {
    redirectUrl = getBase() + "/";
    return SUCCESS;
  }

  /** Where to go after logging in: the page that sent the visitor here, else home; always within this IPT. */
  public void setRedirectUrl() {
    redirectUrl = getBase() + "/";
    // if we have a request, refer back to the originally requested page
    String referer = (String) session.get(Constants.SESSION_REFERER);
    LOG.debug("Session's referer: {}", referer);

    if (StringUtils.isNotEmpty(referer) && !(referer.endsWith("login.do") || referer.endsWith("login"))) {
      redirectUrl = getBase() + LocalPaths.of(referer);
    }

    // remove referer from session
    session.remove(Constants.SESSION_REFERER);

    LOG.info("Redirecting to {}", redirectUrl);
  }

  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  @StrutsParameter
  public void setEmail(String email) {
    this.email = email;
  }

  @StrutsParameter
  public void setPassword(String password) {
    this.password = password;
  }

  @StrutsParameter
  public void setAdminEmail(String adminEmail) {
    this.adminEmail = adminEmail;
  }

}
