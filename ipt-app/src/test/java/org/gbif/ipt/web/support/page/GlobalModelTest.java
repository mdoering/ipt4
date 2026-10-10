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
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.i18n.BundleMessages;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.web.support.feedback.RequestFeedback;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalModelTest {

  private static User user(User.Role role) {
    User u = new User();
    u.setEmail("u@example.org");
    u.setRole(role);
    return u;
  }

  @SuppressWarnings("unchecked")
  private static GlobalModel globalModel(String propsBaseUrl, String configuredBaseUrl, User user) {
    AppConfig cfg = mock(AppConfig.class);
    when(cfg.getBaseUrl()).thenReturn(configuredBaseUrl);
    ObjectProvider<RequestFeedback> feedback = mock(ObjectProvider.class);
    when(feedback.getObject()).thenReturn(new RequestFeedback(Locale.UK, user, null));
    return new GlobalModel(cfg, new IptProperties(null, propsBaseUrl, "en", null, false, null), new BundleMessages(),
        mock(RegistrationManager.class), feedback);
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ipt/about");
    request.setContextPath("/ipt");
    request.setServletPath("/about");
    return request;
  }

  @Test
  void derivesBaseUrlFromRequestIncludingContextPath() {
    PageContext ctx = globalModel(null, null, null).pageContext(request(), Locale.UK);

    assertThat(ctx.baseUrl()).isEqualTo("http://localhost/ipt");
    assertThat(ctx.requestUrl()).isEqualTo("http://localhost/ipt/about");
  }

  @Test
  void theBaseUrlFromSetupWins() {
    PageContext ctx = globalModel(null, "https://ipt.example.org/", null).pageContext(request(), Locale.UK);
    assertThat(ctx.baseUrl()).isEqualTo("https://ipt.example.org");
  }

  @Test
  void theDeploymentBaseUrlWinsOverSetup() {
    PageContext ctx = globalModel("https://proxy.example.org", "https://ipt.example.org", null)
        .pageContext(request(), Locale.UK);
    assertThat(ctx.baseUrl()).isEqualTo("https://proxy.example.org");
  }

  @Test
  void anonymousVisitorsHaveNoRights() {
    PageContext ctx = globalModel(null, null, null).pageContext(request(), Locale.UK);

    assertThat(ctx.loggedIn()).isFalse();
    assertThat(ctx.managerRights()).isFalse();
    assertThat(ctx.adminRights()).isFalse();
  }

  @Test
  void publisherHasManagerButNotAdminRights() {
    PageContext ctx = globalModel(null, null, user(User.Role.Publisher)).pageContext(request(), Locale.UK);

    assertThat(ctx.loggedIn()).isTrue();
    assertThat(ctx.managerRights()).isTrue();
    assertThat(ctx.adminRights()).isFalse();
  }

  @Test
  void adminHasAllRights() {
    PageContext ctx = globalModel(null, null, user(User.Role.Admin)).pageContext(request(), Locale.UK);

    assertThat(ctx.managerRights()).isTrue();
    assertThat(ctx.adminRights()).isTrue();
  }

  @Test
  void unsupportedLocaleRendersBritishEnglish() {
    assertThat(globalModel(null, null, null).pageContext(request(), Locale.GERMAN).locale()).isEqualTo(Locale.UK);
  }
}
