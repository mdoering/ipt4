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
package org.gbif.ipt.web.manage;

import org.gbif.ipt.app.ManageFixture;
import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
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
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's OverviewAction: the overview page, publishing, cancelling, visibility, co-managers, deletion. */
@SpringBootTest
@AutoConfigureMockMvc
class OverviewTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    // a copy of res1 that can be made slow to publish
    FileUtils.copyDirectory(dataDir.resolve("resources/res1").toFile(), dataDir.resolve("resources/big1").toFile());
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired RegistrationManager registrations;

  @BeforeEach
  void owner() throws Exception {
    User owner = ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    ManageFixture.user(users, ManageFixture.OTHER, User.Role.Manager);
    for (String shortname : new String[]{"res1", "big1"}) {
      Resource r = resources.get(shortname);
      if (!r.getManagers().contains(owner)) {
        r.addManager(owner);
      }
    }
  }

  private MockHttpServletResponse as(String email, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
    return mvc.perform(request.with(user(email)).with(csrf())).andReturn().getResponse();
  }

  private void awaitPublication(String shortname) throws Exception {
    long deadline = System.currentTimeMillis() + 120_000;
    while (publications.isLocked(shortname) && System.currentTimeMillis() < deadline) {
      Thread.sleep(100);
    }
    assertThat(publications.isLocked(shortname)).isFalse();
  }

  @Test
  void overviewIsForTheResourcesManagers() throws Exception {
    MockHttpServletResponse owner = as(ManageFixture.OWNER, get("/manage/resource.do?r=res1"));
    assertThat(owner.getStatus()).isEqualTo(200);
    assertThat(owner.getContentAsString()).contains("res1").doesNotContain("FreeMarker template error");
    assertThat(as(ManageFixture.OTHER, get("/manage/resource.do?r=res1")).getStatus()).isEqualTo(401);
    assertThat(as(ManageFixture.OWNER, get("/manage/resource.do?r=nothing")).getStatus()).isEqualTo(404);
    for (String url : new String[]{"/manage/report.do?r=res1", "/manage/cancel.do?r=res1",
        "/manage/mappingPeek.do?r=res1", "/manage/eml.do?r=res1", "/manage/locked.do?r=res1"}) {
      assertThat(as(ManageFixture.OTHER, get(url)).getStatus()).as(url).isEqualTo(401);
    }
  }

  @Test
  void publishFromTheOverview() throws Exception {
    BigDecimal before = resources.get("res1").getEmlVersion();
    MockHttpServletResponse r = as(ManageFixture.OWNER, post("/manage/publish.do").param("r", "res1")
        .param("publish", "Publish").param("summary", "first IPT4 version"));
    assertThat(r.getStatus()).isEqualTo(200);
    awaitPublication("res1");
    Resource res1 = resources.get("res1");
    assertThat(res1.getEmlVersion()).isGreaterThan(before);
    assertThat(mvc.perform(get("/archive.do?r=res1")).andReturn().getResponse().getStatus()).isEqualTo(200);
    assertThat(as(ManageFixture.OWNER, get("/manage/eml.do?r=res1")).getContentType()).startsWith("text/xml");
  }

  @Test
  void lockedWhilePublishingAndCancelKeepsThePreviousVersion() throws Exception {
    as(ManageFixture.OWNER, post("/manage/publish.do").param("r", "big1").param("publish", "Publish"));
    awaitPublication("big1");
    BigDecimal published = resources.get("big1").getEmlVersion();

    // a source big enough to watch the next publication
    Path source = dataDir.resolve("resources/big1/sources/occurrence.txt");
    try (BufferedWriter w = Files.newBufferedWriter(source)) {
      w.write("id\tscientificName\tbasisOfRecord\tkingdom\n");
      for (int i = 0; i < 1_500_000; i++) {
        w.write(i + "\tpuma concolor\toccurrence\tanimalia\n");
      }
    }
    MockHttpServletResponse started = as(ManageFixture.OWNER, post("/manage/publish.do").param("r", "big1")
        .param("publish", "Publish"));
    assertThat(started.getStatus()).isEqualTo(200);
    assertThat(publications.isLocked("big1")).isTrue();
    assertThat(as(ManageFixture.OWNER, get("/manage/resource.do?r=big1")).getRedirectedUrl())
        .isEqualTo("/manage/locked.do");
    assertThat(as(ManageFixture.OWNER, get("/manage/report.do?r=big1")).getStatus()).isEqualTo(200);

    MockHttpServletResponse cancelled = as(ManageFixture.OWNER, post("/manage/cancel.do").param("r", "big1"));
    assertThat(cancelled.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=big1");
    awaitPublication("big1");
    assertThat(resources.get("big1").getEmlVersion()).isEqualTo(published);
    MockHttpServletResponse archive = mvc.perform(get("/archive.do?r=big1")).andReturn().getResponse();
    assertThat(archive.getStatus()).isEqualTo(200);
    assertThat(archive.getHeader("Content-Disposition")).contains("v" + published.toPlainString());
  }

  /** Visibility changes are pending until the next publication, as in IPT 3. */
  @Test
  void visibility() throws Exception {
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    assertThat(as(ManageFixture.OWNER, post("/manage/resource-makePrivate.do").param("r", "res1")
        .param("unpublish", "Make private")).getRedirectedUrl()).isEqualTo("/manage/resource.do?r=res1");
    assertThat(resources.get("res1").getPendingStatus()).isEqualTo(PublicationStatus.PRIVATE);
    as(ManageFixture.OWNER, post("/manage/resource-cancelVisibilityChange.do").param("r", "res1"));
    assertThat(resources.get("res1").getPendingStatus()).isNull();
    assertThat(resources.get("res1").getStatus()).isEqualTo(PublicationStatus.PUBLIC);
    // state changes are POSTs
    assertThat(as(ManageFixture.OWNER, get("/manage/resource-makePrivate.do?r=res1&unpublish=1")).getStatus())
        .isEqualTo(405);
    assertThat(resources.get("res1").getPendingStatus()).isNull();
  }

  @Test
  void coManagers() throws Exception {
    as(ManageFixture.OWNER, post("/manage/resource-addManager.do").param("r", "res1").param("id", ManageFixture.OTHER));
    assertThat(as(ManageFixture.OTHER, get("/manage/resource.do?r=res1")).getStatus()).isEqualTo(200);
    as(ManageFixture.OWNER, post("/manage/resource-deleteManager.do").param("r", "res1")
        .param("id", ManageFixture.OTHER));
    assertThat(as(ManageFixture.OTHER, get("/manage/resource.do?r=res1")).getStatus()).isEqualTo(401);
  }

  @Test
  void replaceEmlAndDeleteFromIpt() throws Exception {
    as(ManageFixture.OWNER, post("/manage/create.do").param("shortname", "del1").param("resourceType", "metadata"));
    MockMultipartFile eml;
    try (InputStream in = getClass().getResourceAsStream("/create/eml.xml")) {
      eml = new MockMultipartFile("emlFile", "eml.xml", "text/xml", in.readAllBytes());
    }
    MockHttpServletResponse replaced = as(ManageFixture.OWNER, multipart("/manage/replace-eml.do").file(eml)
        .param("r", "del1"));
    assertThat(replaced.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=del1");
    assertThat(resources.get("del1").getEml().getTitle()).isEqualTo("WII Herbarium Dataset");

    MockHttpServletResponse deleted = as(ManageFixture.OWNER, post("/manage/resource-deleteFromIpt.do")
        .param("r", "del1").param("deleteFlag", "Delete"));
    assertThat(deleted.getRedirectedUrl()).isEqualTo("/manage/home.do");
    assertThat(resources.get("del1")).isNull();
  }
}
