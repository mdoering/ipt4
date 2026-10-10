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
package org.gbif.ipt.web.security;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.Constants;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * What IPT 3's LoginAction did after a successful login: record the login, apply the session timeout and go back to
 * the page that asked for it (a protected page, else the referring page), always within this IPT.
 */
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final Logger LOG = LogManager.getLogger(LoginSuccessHandler.class);

  private final UserAccountManager users;
  private final AppConfig cfg;
  private final RequestCache requestCache = new HttpSessionRequestCache();

  public LoginSuccessHandler(UserAccountManager users, AppConfig cfg) {
    this.users = users;
    this.cfg = cfg;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
    User user = users.get(authentication.getName());
    if (user != null) {
      LOG.info("User {} logged in successfully", user.getEmail());
      user.setLastLoginToNow();
      users.save();
    }
    HttpSession session = request.getSession();
    session.setMaxInactiveInterval(cfg.getSessionTimeout());
    response.sendRedirect(request.getContextPath() + target(request, response, session));
  }

  private String target(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
    SavedRequest saved = requestCache.getRequest(request, response);
    requestCache.removeRequest(request, response);
    if (saved instanceof DefaultSavedRequest protectedPage) {
      String path = protectedPage.getRequestURI().substring(request.getContextPath().length());
      String query = protectedPage.getQueryString();
      return LocalPaths.of(query == null ? path : path + "?" + query);
    }
    Object referer = session.getAttribute(Constants.SESSION_REFERER);
    session.removeAttribute(Constants.SESSION_REFERER);
    if (referer instanceof String path && !(path.endsWith("login.do") || path.endsWith("login"))) {
      return LocalPaths.of(path);
    }
    return "/";
  }
}
