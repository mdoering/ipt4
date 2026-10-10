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
package org.gbif.ipt.web.support.page;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.Constants;
import org.gbif.ipt.feedback.Feedback;
import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.User;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The view half of the IPT 3 {@code BaseAction}: templates reach these getters as top-level variables
 * ({@code ${baseURL}}) and through {@code action}. Page-specific beans extend this class.
 */
public class PageSupport {

  private final PageContext ctx;

  public PageSupport(PageContext ctx) {
    this.ctx = ctx;
  }

  protected PageContext context() {
    return ctx;
  }

  public AppConfig getCfg() {
    return ctx.cfg();
  }

  public String getBase() {
    return ctx.baseUrl();
  }

  public String getBaseURL() {
    return ctx.baseUrl();
  }

  public String getLogoRedirectURL() {
    String url = ctx.cfg().getLogoRedirectUrl();
    return url != null ? url : ctx.baseUrl();
  }

  /** The current URL without {@code request_locale}, for the language menu. */
  public String getRequestURL() {
    return ctx.requestUrl();
  }

  public Locale getLocale() {
    return ctx.locale();
  }

  public String getLocaleLanguage() {
    return ctx.locale().getLanguage();
  }

  public boolean isLoggedIn() {
    return ctx.loggedIn();
  }

  public boolean isAdminRights() {
    return ctx.adminRights();
  }

  public boolean isManagerRights() {
    return ctx.managerRights();
  }

  /** The logged-in IPT user, or null for visitors. */
  @Nullable
  public User getCurrentUser() {
    return ctx.currentUser();
  }

  /** The registered IPT installation, or null if the IPT is not registered. */
  @Nullable
  public Ipt getRegisteredIpt() {
    return ctx.registrations().getIpt();
  }

  /** The "No organisation" organisation, or null. */
  @Nullable
  public Organisation getDefaultOrganisation() {
    return ctx.registrations().get(Constants.DEFAULT_ORG_KEY);
  }

  /** Where messages, warnings and errors of this request go. */
  public Feedback getFeedback() {
    return ctx.feedback();
  }

  public List<String> getActionWarnings() {
    return ctx.feedback().getActionWarnings();
  }

  public List<String> getWarnings() {
    return ctx.feedback().getActionWarnings();
  }

  public List<String> getActionErrors() {
    return ctx.feedback().getActionErrors();
  }

  public List<String> getActionMessages() {
    return ctx.feedback().getActionMessages();
  }

  public String getPrimaryColor() {
    return toRgb(ctx.cfg().getColorSchemeConfig().getPrimaryColor());
  }

  public String getNavbarColor() {
    return toRgb(ctx.cfg().getColorSchemeConfig().getNavbarColor());
  }

  public String getNavbarLinkColor() {
    return toRgb(ctx.cfg().getColorSchemeConfig().getNavbarLinkColor());
  }

  public String getNavbarGbifLogoColor() {
    return toRgb(ctx.cfg().getColorSchemeConfig().getNavbarGbifLogoColor());
  }

  public String getNavbarActiveTabColor() {
    return toRgb(ctx.cfg().getColorSchemeConfig().getNavbarActiveTabColor());
  }

  public String getLinkColor() {
    return toRgb(ctx.cfg().getColorSchemeConfig().getLinkColor());
  }

  /** {@code "#61a861"} to {@code "97,168,97"}, the form the CSS variables in inc/header.ftl expect. */
  static String toRgb(String hex) {
    return Integer.valueOf(hex.substring(1, 3), 16) + "," + Integer.valueOf(hex.substring(3, 5), 16) + ","
        + Integer.valueOf(hex.substring(5, 7), 16);
  }

  public String getText(String key) {
    return ctx.messages().get(ctx.locale(), key);
  }

  public String getText(String key, List<?> args) {
    return ctx.messages().get(ctx.locale(), key, args.toArray());
  }

  public String getText(String key, String[] args) {
    return ctx.messages().get(ctx.locale(), key, (Object[]) args);
  }

  public String getText(String key, String defaultValue) {
    return ctx.messages().getOrDefault(ctx.locale(), key, defaultValue);
  }

  public String getText(String key, String defaultValue, List<?> args) {
    return ctx.messages().getOrDefault(ctx.locale(), key, defaultValue, args.toArray());
  }

  public String getText(String key, String defaultValue, String[] args) {
    return ctx.messages().getOrDefault(ctx.locale(), key, defaultValue, (Object[]) args);
  }

  public String getTextWithDynamicArgs(String key, String... args) {
    return ctx.messages().get(ctx.locale(), key, (Object[]) args);
  }
}
