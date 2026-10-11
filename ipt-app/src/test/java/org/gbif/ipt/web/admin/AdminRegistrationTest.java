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
package org.gbif.ipt.web.admin;

import org.gbif.ipt.app.ManageFixture;
import org.gbif.ipt.app.StubRegistry;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's OrganisationsAction and RegistrationAction against a stub registry; one IPT, so the steps run in order. */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminRegistrationTest {

  private static final String ADMIN = "registration.admin@example.org";
  private static final String GBIFS = "fbca90e3-8aed-48b1-84e3-369afbd000ce";
  private static final String ORG_PASSWORD = "org-secret-1";
  private static final String IPT_KEY = "7e5bd5f4-3b7a-4c5e-9d3e-0a1b2c3d4e5f";

  static Path dataDir;
  static StubRegistry stub;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
    stub = StubRegistry.start(registry);
    stub.organisationPassword(GBIFS, ORG_PASSWORD);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired RegistrationManager registrations;

  @BeforeEach
  void admin() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
  }

  private String page(String url) throws Exception {
    MockHttpServletResponse response = mvc.perform(get(URI.create(url)).with(user(ADMIN))).andReturn().getResponse();
    assertThat(response.getStatus()).as(url).isEqualTo(200);
    assertThat(response.getContentAsString()).as(url).doesNotContain("FreeMarker template error");
    return response.getContentAsString();
  }

  private MockHttpServletResponse send(String url, String... params) throws Exception {
    var request = post(URI.create(url)).with(user(ADMIN)).with(csrf());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private String[] organisation(String password, String alias) {
    return new String[] {"organisation.key", GBIFS, "organisation.name", "GBIF Secretariat",
        "organisation.password", password, "organisation.alias", alias, "organisation.canHost", "true",
        "organisation.primaryContactEmail", "helpdesk@gbif.org", "save", "Save"};
  }

  @Test
  @Order(1)
  void aWrongPasswordIsRefused() throws Exception {
    assertThat(page("/admin/organisation.do")).contains("GBIF Secretariat");
    MockHttpServletResponse refused = send("/admin/organisation.do", organisation("wrong", "GBIFS"));
    assertThat(refused.getRedirectedUrl()).isNull();
    assertThat(registrations.get(GBIFS)).isNull();
  }

  @Test
  @Order(2)
  void associateAnOrganisation() throws Exception {
    MockHttpServletResponse added = send("/admin/organisation.do", organisation(ORG_PASSWORD, "GBIFS"));
    assertThat(added.getRedirectedUrl()).isEqualTo("/admin/organisations.do");
    assertThat(registrations.get(GBIFS).getPassword()).isEqualTo(ORG_PASSWORD);
    // the list shows once the IPT is registered
    assertThat(page("/admin/organisations.do")).doesNotContain("GBIF Secretariat");
  }

  /** IPT 3 wrote the passwords into the edit form; an empty field keeps the stored one. */
  @Test
  @Order(3)
  void passwordsAreNotShownAndAreKept() throws Exception {
    String html = page("/admin/organisation.do?id=" + GBIFS);
    assertThat(html).doesNotContain(ORG_PASSWORD);
    MockHttpServletResponse saved = send("/admin/organisation.do?id=" + GBIFS, "id", GBIFS,
        "organisation.key", GBIFS, "organisation.name", "GBIF Secretariat", "organisation.password", "",
        "organisation.alias", "Secretariat", "organisation.canHost", "true", "save", "Save");
    assertThat(saved.getRedirectedUrl()).isEqualTo("/admin/organisations.do");
    assertThat(registrations.get(GBIFS).getPassword()).isEqualTo(ORG_PASSWORD);
    assertThat(registrations.get(GBIFS).getAlias()).isEqualTo("Secretariat");
    assertThat(Files.readString(dataDir.resolve("config/registration.json"))).doesNotContain(ORG_PASSWORD);
  }

  @Test
  @Order(4)
  void synchronise() throws Exception {
    assertThat(send("/admin/organisationsSynchronize.do").getStatus()).isEqualTo(200);
    assertThat(registrations.get(GBIFS)).isNotNull();
  }

  @Test
  @Order(5)
  void registerTheIpt() throws Exception {
    page("/admin/registration.do");
    stub.answer("POST", "/registry/ipt/register", 201, "application/xml",
        "<IptEntry><key>" + IPT_KEY + "</key></IptEntry>");
    // the page's script copies the chosen organisation into ipt.organisationKey
    MockHttpServletResponse registered = send("/admin/registration.do", "organisation.key", GBIFS,
        "ipt.organisationKey", GBIFS, "organisation.name", "GBIF Secretariat", "organisation.canHost", "true",
        "organisation.password", ORG_PASSWORD, "ipt.name", "Test IPT", "ipt.description", "An IPT for tests",
        "ipt.primaryContactName", "Ann Lee", "ipt.primaryContactEmail", "ann.lee@example.org",
        "ipt.primaryContactType", "technical", "ipt.wsPassword", "ws-secret-1", "save", "Save");
    assertThat(registered.getRedirectedUrl()).isEqualTo("/home.do");
    assertThat(registrations.getIpt().getKey()).hasToString(IPT_KEY);
    assertThat(registrations.getHostingOrganisation().getKey()).hasToString(GBIFS);
    StubRegistry.Request request = stub.requests().stream().filter(r -> r.path().equals("/registry/ipt/register"))
        .findFirst().orElseThrow();
    assertThat(request.body()).contains("name=Test+IPT").contains("primaryContactEmail=ann.lee%40example.org")
        .contains("wsPassword=ws-secret-1");
    assertThat(Files.readString(dataDir.resolve("config/registration.json"))).doesNotContain("ws-secret-1");
    assertThat(page("/admin/organisations.do")).contains("GBIF Secretariat");
  }

  /** IPT 3 bound registeredIpt.* on every page; here only the update form, and a rejected update keeps the IPT. */
  @Test
  @Order(6)
  void updateTheRegistration() throws Exception {
    stub.answer("POST", "/registry/ipt/update/" + IPT_KEY, 200, "text/plain", "");
    MockHttpServletResponse rejected = send("/admin/updateRegistration.do", "registeredIpt.name", "",
        "registeredIpt.description", "Changed", "update", "Update");
    assertThat(rejected.getRedirectedUrl()).isNull();
    assertThat(registrations.getIpt().getDescription()).isEqualTo("An IPT for tests");

    MockHttpServletResponse updated = send("/admin/updateRegistration.do", "registeredIpt.name", "Test IPT",
        "registeredIpt.description", "Changed", "registeredIpt.primaryContactName", "Ann Lee",
        "registeredIpt.primaryContactEmail", "ann.lee@example.org", "update", "Update");
    assertThat(updated.getRedirectedUrl()).isEqualTo("/home.do");
    assertThat(registrations.getIpt().getDescription()).isEqualTo("Changed");
    assertThat(stub.requests()).anyMatch(r -> r.path().equals("/registry/ipt/update/" + IPT_KEY)
        && r.body().contains("description=Changed"));

    // other pages do not bind it
    send("/admin/config.do", "registeredIpt.description", "Hijacked", "save", "Save");
    assertThat(registrations.getIpt().getDescription()).isEqualTo("Changed");
  }

  @Test
  @Order(7)
  void changeTokens() throws Exception {
    MockHttpServletResponse changed = send("/admin/changeTokens.do", "tokenChange", "true",
        "registeredIptPassword", "new-ws-secret", "hostingOrganisationToken", "");
    assertThat(changed.getRedirectedUrl()).isEqualTo("/home.do");
    assertThat(registrations.getIpt().getWsPassword()).isEqualTo("new-ws-secret");
  }

  @Test
  @Order(8)
  void anOrganisationInUseByTheIptIsKept() throws Exception {
    send("/admin/organisation.do", "id", GBIFS, "deleteFlag", "Delete");
    assertThat(registrations.get(GBIFS)).isNotNull();
  }
}
