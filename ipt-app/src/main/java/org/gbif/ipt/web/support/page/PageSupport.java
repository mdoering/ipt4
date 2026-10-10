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

  public SiteConfig getCfg() {
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

  /** The logged-in IPT user; user accounts arrive in M2. */
  public Object getCurrentUser() {
    return null;
  }

  /** The registered IPT installation; registration arrives in M1. */
  public Object getRegisteredIpt() {
    return null;
  }

  public Object getDefaultOrganisation() {
    return null;
  }

  public List<String> getActionWarnings() {
    return List.of();
  }

  public List<String> getActionErrors() {
    return List.of();
  }

  public List<String> getActionMessages() {
    return List.of();
  }

  public String getPrimaryColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().primaryColor());
  }

  public String getNavbarColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarColor());
  }

  public String getNavbarLinkColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarLinkColor());
  }

  public String getNavbarGbifLogoColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarGbifLogoColor());
  }

  public String getNavbarActiveTabColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().navbarActiveTabColor());
  }

  public String getLinkColor() {
    return ColorScheme.toRgb(ctx.cfg().getColorSchemeConfig().linkColor());
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
