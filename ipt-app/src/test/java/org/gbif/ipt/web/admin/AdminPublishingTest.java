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
import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;

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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's BulkPublicationAction, PublishAllResourcesAction, PublishingStatus(Api)Action and DeleteVersionAction. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminPublishingTest {

  private static final String ADMIN = "publishing.admin@example.org";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired RegistrationManager registrations;

  /** res1 (IPT 3's fixture) lacks required metadata; eml1 has all of it. */
  @BeforeEach
  void resources() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    if (resources.get("eml1") == null) {
      try (InputStream in = getClass().getResourceAsStream("/create/eml.xml")) {
        mvc.perform(multipart("/manage/create.do").file(new MockMultipartFile("file", "eml.xml", "text/xml",
                in.readAllBytes())).param("shortname", "eml1").param("resourceType", "occurrence")
            .with(user(ADMIN)).with(csrf()));
      }
    }
    assertThat(resources.get("eml1")).isNotNull();
  }

  private MockHttpServletResponse publishSelected(String shortname) throws Exception {
    return mvc.perform(post("/admin/publishAll.do").param("publishMode", "SELECTED")
        .param("selectedResources", shortname).param("publishAll", "Publish").with(user(ADMIN)).with(csrf()))
        .andReturn().getResponse();
  }

  @Test
  void bulkPublicationPublishesTheSelectedResources() throws Exception {
    String html = mvc.perform(get("/admin/bulk-publication.do").with(user(ADMIN))).andReturn().getResponse()
        .getContentAsString();
    assertThat(Jsoup.parse(html).selectFirst("form[action=publishAll.do] input[name=_csrf]")).isNotNull();
    int before = resources.get("eml1").getVersionHistory().size();
    MockHttpServletResponse done = publishSelected("eml1");
    assertThat(done.getStatus()).isEqualTo(200);
    assertThat(done.getContentAsString()).doesNotContain("FreeMarker template error")
        .contains("Publish all resources completed.");
    assertThat(publications.isLocked("eml1")).isFalse();
    assertThat(resources.get("eml1").getVersionHistory()).hasSize(before + 1);
    assertThat(resources.get("eml1").getLastPublished()).isNotNull();

    // incomplete metadata: not published, with IPT 3's message
    BigDecimal res1 = resources.get("res1").getEmlVersion();
    assertThat(publishSelected("res1").getContentAsString())
        .contains("you must provide the required basic metadata and contacts");
    assertThat(resources.get("res1").getEmlVersion()).isEqualTo(res1);
  }

  @Test
  void statusPageAndApi() throws Exception {
    publishSelected("eml1");
    assertThat(mvc.perform(get("/admin/bulkReport.do").with(user(ADMIN))).andReturn().getResponse()
        .getContentAsString()).contains("eml1");
    JsonNode report = JsonMapper.builder().build().readTree(mvc.perform(get("/admin-api/publication-report.do")
        .param("r", "eml1").with(user(ADMIN))).andReturn().getResponse().getContentAsString());
    assertThat(report.get("completed").asBoolean()).isTrue();
    assertThat(report.get("state").asString()).isNotEmpty();
    assertThat(report.get("messages").get(0).get("level").get("standardLevel").asString()).isNotEmpty();
    assertThat(report.get("messages").get(0).get("timestamp").asLong()).isPositive();
    assertThat(report.has("exception")).isFalse();
    JsonNode reports = JsonMapper.builder().build().readTree(mvc.perform(get("/admin-api/publication-reports.do")
        .with(user(ADMIN))).andReturn().getResponse().getContentAsString());
    assertThat(reports.has("eml1")).isTrue();
  }

  @Test
  void deleteAnOlderVersionByPostOnly() throws Exception {
    publishSelected("eml1");
    publishSelected("eml1");
    BigDecimal latest = resources.get("eml1").getEmlVersion();
    BigDecimal older = resources.get("eml1").getVersionHistory().stream().map(h -> new BigDecimal(h.getVersion()))
        .filter(v -> v.compareTo(latest) < 0).findFirst().orElseThrow();
    String url = "/admin/deleteVersion.do?r=eml1&v=" + older.toPlainString();
    assertThat(mvc.perform(get(URI.create(url)).with(user(ADMIN))).andReturn().getResponse().getStatus())
        .isEqualTo(405);
    assertThat(resources.get("eml1").getVersionHistory()).anyMatch(h -> new BigDecimal(h.getVersion()).equals(older));

    MockHttpServletResponse deleted = mvc.perform(post(URI.create(url)).with(user(ADMIN)).with(csrf())).andReturn()
        .getResponse();
    assertThat(deleted.getRedirectedUrl()).isEqualTo("/resource?r=eml1");
    assertThat(resources.get("eml1").getVersionHistory()).noneMatch(h -> new BigDecimal(h.getVersion()).equals(older));

    // the resource page's delete links post
    String page = mvc.perform(get("/resource.do?r=res1&v=" + resources.get("res1").getEmlVersion().toPlainString())
        .with(user(ADMIN))).andReturn()
        .getResponse().getContentAsString();
    Jsoup.parse(page).select("a.confirmDeleteVersion").forEach(a -> assertThat(a.attr("data-method")).isEqualTo("post"));
  }

  @Test
  void unknownResourceOrVersionIs404() throws Exception {
    assertThat(mvc.perform(post("/admin/deleteVersion.do?r=nothing&v=1.0").with(user(ADMIN)).with(csrf())).andReturn()
        .getResponse().getStatus()).isEqualTo(404);
    assertThat(mvc.perform(post("/admin/deleteVersion.do?r=res1&v=x").with(user(ADMIN)).with(csrf())).andReturn()
        .getResponse().getStatus()).isEqualTo(404);
  }
}
