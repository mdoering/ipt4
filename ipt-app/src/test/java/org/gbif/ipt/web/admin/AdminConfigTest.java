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
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.ConfigManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.web.BrowserForms;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** IPT 3's ConfigAction, UIManagementAction and LogsAction. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminConfigTest {

  private static final String ADMIN = "config.admin@example.org";
  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0};

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired AppConfig cfg;
  @Autowired ConfigManager configManager;
  @Autowired DataDir dir;

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

  private MvcResult save(String url, String formAction, Map<String, String> changed) throws Exception {
    return mvc.perform(BrowserForms.submit(Jsoup.parse(page(url), "http://localhost" + url), formAction, null, "",
        changed).with(user(ADMIN)).with(csrf())).andReturn();
  }

  /** The settings in ipt.properties; a save also writes the settings it does not have as empty ones. */
  private Map<String, String> iptProperties() throws Exception {
    Properties p = new Properties();
    try (InputStream in = new FileInputStream(dir.configFile("ipt.properties"))) {
      p.load(in);
    }
    Map<String, String> set = new TreeMap<>();
    p.stringPropertyNames().stream().filter(k -> !p.getProperty(k).isEmpty()).forEach(k -> set.put(k, p.getProperty(k)));
    return set;
  }

  @Test
  void configSurvivesAnUnchangedSave() throws Exception {
    configManager.setMailSettings("smtp.example.org", "587", "mailer", "smtp-secret-1", true);
    configManager.saveConfig();
    Map<String, String> before = iptProperties();
    String html = page("/admin/config.do");
    assertThat(html).doesNotContain("smtp-secret-1");

    MvcResult saved = save("/admin/config.do", "config.do", Map.of("mailSmtpPassword", ""));
    assertThat(saved.getResponse().getRedirectedUrl()).isEqualTo("/admin/config.do");
    // the empty password field keeps the stored password
    assertThat(iptProperties()).isEqualTo(before);
    assertThat(cfg.getMailSmtpPassword()).isEqualTo("smtp-secret-1");
  }

  @Test
  void changedSettingsAreSaved() throws Exception {
    save("/admin/config.do", "config.do", Map.of("adminEmail", "ipt-admin@example.org", "archivalLimit", "7"));
    assertThat(cfg.getAdminEmail()).isEqualTo("ipt-admin@example.org");
    assertThat(cfg.getArchivalLimit()).isEqualTo(7);
    assertThat(iptProperties().get(AppConfig.ADMIN_EMAIL)).isEqualTo("ipt-admin@example.org");
  }

  @Test
  void anInvalidBaseUrlIsRefused() throws Exception {
    String baseUrl = cfg.getBaseUrl();
    MvcResult refused = save("/admin/config.do", "config.do", Map.of("baseUrl", "not a url"));
    assertThat(refused.getResponse().getRedirectedUrl()).isNull();
    assertThat(refused.getResponse().getContentAsString()).contains("The Public URL entered is not of the valid form");
    assertThat(cfg.getBaseUrl()).isEqualTo(baseUrl);
  }

  @Test
  void coloursShowInTheTheme() throws Exception {
    MvcResult saved = save("/admin/uiManagement.do", "uiManagement.do", Map.of("colorScheme.primaryColor", "#123456"));
    assertThat(saved.getResponse().getRedirectedUrl()).isEqualTo("/admin/uiManagement.do");
    assertThat(page("/about.do")).contains("--color-gbif-primary: 18,52,86");
  }

  @Test
  void appLogoUpload() throws Exception {
    mvc.perform(multipart("/admin/appLogoUpload.do").file(new MockMultipartFile("file", "../../logo.png", "image/png",
        PNG)).with(user(ADMIN)).with(csrf())).andReturn();
    assertThat(dir.appLogoFile("png")).exists();
    MockHttpServletResponse logo = mvc.perform(get("/appLogo.do")).andReturn().getResponse();
    assertThat(logo.getStatus()).isEqualTo(200);
    assertThat(logo.getContentAsByteArray()).isEqualTo(PNG);

    // not an image: refused, the logo stays
    mvc.perform(multipart("/admin/appLogoUpload.do").file(new MockMultipartFile("file", "logo.html", "text/html",
        "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))).with(user(ADMIN)).with(csrf())).andReturn();
    assertThat(dir.appLogoFile("png")).exists();
    assertThat(dir.appLogoFile("html")).doesNotExist();
    assertThat(mvc.perform(get("/appLogo.do")).andReturn().getResponse().getContentAsByteArray()).isEqualTo(PNG);
  }

  @Test
  void logsPageAndLogFiles() throws Exception {
    Files.createDirectories(dir.loggingDir().toPath());
    Files.writeString(dir.loggingFile("admin.log").toPath(), "admin log line");
    assertThat(page("/admin/logs.do")).contains("admin");
    MockHttpServletResponse log = mvc.perform(get("/admin/logfile.do?log=admin").with(user(ADMIN)))
        .andReturn().getResponse();
    assertThat(log.getStatus()).isEqualTo(200);
    assertThat(log.getContentAsString()).isEqualTo("admin log line");
  }

  @Test
  void logfileStaysInLogsFolder() throws Exception {
    Files.writeString(dataDir.resolve("config/secret.log"), "not a log");
    for (String log : new String[] {"../config/secret", "..%2Fconfig%2Fsecret", "/etc/hosts", "%2e%2e/config/secret",
        "nothing"}) {
      MockHttpServletResponse response = mvc.perform(get(URI.create("/admin/logfile.do?log=" + log))
          .with(user(ADMIN))).andReturn().getResponse();
      assertThat(response.getStatus()).as(log).isEqualTo(404);
      assertThat(response.getContentAsString()).as(log).doesNotContain("not a log");
    }
  }
}
