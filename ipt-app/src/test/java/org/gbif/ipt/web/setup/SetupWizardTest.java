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
package org.gbif.ipt.web.setup;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.ConfigWarnings;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.ConfigManager;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * IPT 3's setup steps 2 to 5 on a fresh data dir, and the guard that sends every other page there until setup is
 * complete. Runs on a real port: step 4 checks that the IPT answers at the entered base URL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "test.ipt.complete-setup=false")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SetupWizardTest {

  private static final String ADMIN = "first.admin@example.org";
  private static final String STEP2 = "/setupDefaultAdministrator.do";

  static Path dataDir;

  @DynamicPropertySource
  static void freshDataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = Files.createTempDirectory("ipt-setup");
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @LocalServerPort int port;
  @Autowired MockMvc mvc;
  @Autowired ConfigManager configManager;
  @Autowired AppConfig cfg;
  @Autowired UserAccountManager users;
  @Autowired ConfigWarnings warnings;

  private MockHttpServletResponse perform(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request).andReturn().getResponse();
  }

  private static String loggedInAs(MvcResult result) {
    SecurityContext ctx = (SecurityContext) result.getRequest().getSession()
        .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    return ctx == null || ctx.getAuthentication() == null ? null : ctx.getAuthentication().getName();
  }

  @Test
  @Order(1)
  void pagesWaitForSetup() throws Exception {
    assertThat(configManager.setupComplete()).isFalse();
    for (String url : new String[]{"/", "/about.do", "/resource?r=x", "/manage/", "/admin/", "/inventory/dataset"}) {
      assertThat(perform(get(url)).getRedirectedUrl()).as(url).isEqualTo(STEP2);
    }
    for (String url : new String[]{STEP2, "/styles/main.css", "/actuator/health", "/api/health", "/login"}) {
      assertThat(perform(get(url)).getStatus()).as(url).isEqualTo(200);
    }
  }

  /** Over a real connection, where Tomcat sends the first part of a long page before the form's CSRF token. */
  @Test
  @Order(1)
  void formsRenderForNewVisitors() throws Exception {
    java.net.http.HttpResponse<String> page = java.net.http.HttpClient.newHttpClient().send(
        java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + STEP2)).build(),
        java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(page.statusCode()).isEqualTo(200);
    assertThat(page.body()).contains("name=\"_csrf\"").contains("</html>");
  }

  @Test
  @Order(2)
  void freshInstallWalksThroughAllSteps() throws Exception {
    MockHttpSession session = new MockHttpSession();
    MockHttpServletResponse mismatch = perform(post(STEP2).session(session).with(csrf())
        .param("setupDefaultAdministrator", "true").param("user.firstname", "First").param("user.lastname", "Admin")
        .param("user.email", ADMIN).param("user.password", "Secret-pass-1").param("password2", "other"));
    assertThat(mismatch.getStatus()).isEqualTo(200);
    assertThat(mismatch.getContentAsString()).contains("The passwords provided don");
    assertThat(users.get(ADMIN)).isNull();

    String anonymousSessionId = session.getId();
    MvcResult admin = mvc.perform(post(STEP2).session(session).with(csrf())
        .param("setupDefaultAdministrator", "true").param("user.firstname", "First").param("user.lastname", "Admin")
        .param("user.email", ADMIN).param("user.password", "Secret-pass-1").param("password2", "Secret-pass-1"))
        .andReturn();
    assertThat(admin.getResponse().getRedirectedUrl()).endsWith("/setupMode.do");
    assertThat(users.get(ADMIN).getRole()).isEqualTo(User.Role.Admin);
    assertThat(loggedInAs(admin)).isEqualTo(ADMIN);
    MockHttpSession adminSession = (MockHttpSession) admin.getRequest().getSession();
    assertThat(adminSession.getId()).isNotEqualTo(anonymousSessionId);

    assertThat(perform(get("/setupMode.do").session(adminSession)).getStatus()).isEqualTo(200);
    assertThat(perform(post("/setupMode.do").session(adminSession).with(csrf()).param("modeSelected", "Test"))
        .getRedirectedUrl()).endsWith("/setupPublicUrl.do");
    assertThat(cfg.getRegistryType()).isEqualTo(AppConfig.REGISTRY_TYPE.DEVELOPMENT);

    String baseUrl = "http://localhost:" + port;
    assertThat(perform(get("/setupPublicUrl.do").session(adminSession)).getStatus()).isEqualTo(200);
    MockHttpServletResponse publicUrl = perform(post("/setupPublicUrl.do").session(adminSession).with(csrf())
        .param("setupPublicUrl", "true").param("baseURL", baseUrl).param("proxy", ""));
    assertThat(publicUrl.getRedirectedUrl()).as(publicUrl.getContentAsString()).endsWith("/setupInstallationComplete.do");
    assertThat(cfg.getBaseUrl()).isEqualTo(baseUrl);

    // IPT 3 logged in whoever reached the last step first as the new admin
    assertThat(loggedInAs(mvc.perform(get("/setupInstallationComplete.do")).andReturn())).isNull();
    assertThat(perform(get("/setupInstallationComplete.do").session(adminSession)).getStatus()).isEqualTo(200);
    assertThat(configManager.setupComplete()).isTrue();
    assertThat(perform(get("/about.do")).getStatus()).isEqualTo(200);
    assertThat(perform(post("/setupComplete.do").session(adminSession).with(csrf())).getRedirectedUrl())
        .endsWith("/admin/");
  }

  @Test
  @Order(3)
  void setupIsClosedAfterCompletion() throws Exception {
    assertThat(configManager.setupComplete()).isTrue();
    String baseUrl = cfg.getBaseUrl();

    MvcResult intruder = mvc.perform(post(STEP2).with(csrf()).param("setupDefaultAdministrator", "true")
        .param("user.firstname", "In").param("user.lastname", "Truder").param("user.email", "intruder@example.org")
        .param("user.password", "Secret-pass-1").param("password2", "Secret-pass-1")).andReturn();
    assertThat(users.get("intruder@example.org")).isNull();
    assertThat(loggedInAs(intruder)).isNull();

    mvc.perform(post("/setupMode.do").with(csrf()).param("modeSelected", "Production")).andReturn();
    assertThat(cfg.getRegistryType()).isEqualTo(AppConfig.REGISTRY_TYPE.DEVELOPMENT);

    mvc.perform(post("/setupPublicUrl.do").with(csrf()).param("setupPublicUrl", "true")
        .param("baseURL", "https://evil.example.org")).andReturn();
    assertThat(cfg.getBaseUrl()).isEqualTo(baseUrl);

    MvcResult complete = mvc.perform(get("/setupInstallationComplete.do")).andReturn();
    assertThat(loggedInAs(complete)).isNull();
  }

  @Test
  @Order(4)
  void startupWarningsAreShownOnEveryPage() throws Exception {
    // the page shows the first three warnings; the registry of this offline test is already one of them
    java.util.List<String> atStartup = new java.util.ArrayList<>(warnings.getStartupErrors());
    warnings.getStartupErrors().clear();
    warnings.addStartupError("Registry down");
    try {
      // pages with IPT 3's alert block, e.g. home (about has none)
      assertThat(perform(get("/home.do")).getContentAsString()).contains("IPT startup warnings.")
          .contains("Registry down");
      // in the language chosen with this very request
      assertThat(perform(get("/home.do?request_locale=fr")).getContentAsString())
          .contains("Des avertissements sont survenus au démarrage");
    } finally {
      warnings.getStartupErrors().clear();
      warnings.getStartupErrors().addAll(atStartup);
    }
  }
}
