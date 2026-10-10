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

import org.gbif.ipt.app.IptProperties;
import org.gbif.ipt.i18n.BundleMessages;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalModelTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static GlobalModel globalModel(String baseUrl) {
    SiteConfig cfg =
        new SiteConfig(new IptProperties(null, baseUrl, "en", null, false), Optional.empty(), Optional.empty());
    return new GlobalModel(cfg, new BundleMessages());
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ipt/about");
    request.setContextPath("/ipt");
    request.setServletPath("/about");
    return request;
  }

  @Test
  void derivesBaseUrlFromRequestIncludingContextPath() {
    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.baseUrl()).isEqualTo("http://localhost/ipt");
    assertThat(ctx.requestUrl()).isEqualTo("http://localhost/ipt/about");
  }

  @Test
  void configuredBaseUrlWins() {
    PageContext ctx = globalModel("https://ipt.example.org/").pageContext(request(), Locale.UK);

    assertThat(ctx.baseUrl()).isEqualTo("https://ipt.example.org");
  }

  @Test
  void anonymousVisitorsHaveNoRights() {
    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.loggedIn()).isFalse();
    assertThat(ctx.managerRights()).isFalse();
    assertThat(ctx.adminRights()).isFalse();
  }

  @Test
  void publisherHasManagerButNotAdminRights() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "p@example.org", "n/a", AuthorityUtils.createAuthorityList("ROLE_PUBLISHER")));

    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.loggedIn()).isTrue();
    assertThat(ctx.managerRights()).isTrue();
    assertThat(ctx.adminRights()).isFalse();
  }

  @Test
  void adminHasAllRights() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "a@example.org", "n/a", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

    PageContext ctx = globalModel(null).pageContext(request(), Locale.UK);

    assertThat(ctx.managerRights()).isTrue();
    assertThat(ctx.adminRights()).isTrue();
  }

  @Test
  void unsupportedLocaleRendersBritishEnglish() {
    assertThat(globalModel(null).pageContext(request(), Locale.GERMAN).locale()).isEqualTo(Locale.UK);
  }
}
