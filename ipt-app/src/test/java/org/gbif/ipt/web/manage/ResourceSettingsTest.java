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
import org.gbif.ipt.config.Constants;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.PublicationMode;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's publication settings, auto-publishing, version history and the managers' resource page. */
@SpringBootTest
@AutoConfigureMockMvc
class ResourceSettingsTest {

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

  @BeforeEach
  void publishedAndManaged() throws Exception {
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    User owner = ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    ManageFixture.user(users, ManageFixture.OTHER, User.Role.Manager);
    Resource res1 = resources.get("res1");
    if (!res1.getManagers().contains(owner)) {
      res1.addManager(owner);
    }
  }

  private MockHttpServletResponse as(String email, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
    return mvc.perform(request.with(user(email)).with(csrf())).andReturn().getResponse();
  }

  @Test
  void publicationSettings() throws Exception {
    assertThat(as(ManageFixture.OWNER, get("/manage/publication-settings.do?r=res1")).getStatus()).isEqualTo(200);
    MockHttpServletResponse saved = as(ManageFixture.OWNER, post("/manage/publication-settings.do").param("r", "res1")
        .param("id", Constants.DEFAULT_ORG_KEY.toString()).param("save", "Save"));
    assertThat(saved.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=res1");
    assertThat(resources.get("res1").getOrganisation().getKey().toString())
        .isEqualTo(Constants.DEFAULT_ORG_KEY.toString());
    MockHttpServletResponse unknown = as(ManageFixture.OWNER, post("/manage/publication-settings.do")
        .param("r", "res1").param("id", "00000000-0000-0000-0000-000000000000").param("save", "Save"));
    assertThat(unknown.getStatus()).isEqualTo(200);
    assertThat(as(ManageFixture.OTHER, get("/manage/publication-settings.do?r=res1")).getStatus()).isEqualTo(401);
  }

  @Test
  void autoPublishing() throws Exception {
    assertThat(as(ManageFixture.OWNER, get("/manage/auto-publish.do?r=res1")).getStatus()).isEqualTo(200);
    MockHttpServletResponse on = as(ManageFixture.OWNER, post("/manage/auto-publish.do").param("r", "res1")
        .param("updateFrequency", "monthly").param("updateFrequencyDay", "1").param("updateFrequencyTime", "10:30")
        .param("save", "Save"));
    assertThat(on.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=res1");
    Resource res1 = resources.get("res1");
    assertThat(res1.getPublicationMode()).isEqualTo(PublicationMode.AUTO_PUBLISH_ON);
    assertThat(res1.getNextPublished()).isNotNull();
    as(ManageFixture.OWNER, post("/manage/auto-publish.do").param("r", "res1").param("updateFrequency", "off")
        .param("updateFrequencyDay", "1").param("save", "Save"));
    assertThat(resources.get("res1").getPublicationMode()).isEqualTo(PublicationMode.AUTO_PUBLISH_OFF);
  }

  @Test
  void hugeVersionNumbersAreRefusedOnTheHistoryPage() throws Exception {
    for (String query : new String[]{"v=1e5000000", "version=1e5000000"}) {
      MockHttpServletResponse r = as(ManageFixture.OWNER, get("/manage/history.do?r=res1&" + query));
      assertThat(r.getContentAsByteArray().length).as(query).isLessThan(1_000_000);
    }
  }

  @Test
  void versionHistoryAndManagersResourcePage() throws Exception {
    String version = resources.get("res1").getEmlVersion().toPlainString();
    assertThat(as(ManageFixture.OWNER, get("/manage/history.do?r=res1&v=" + version)).getStatus()).isEqualTo(200);
    MockHttpServletResponse saved = as(ManageFixture.OWNER, post("/manage/history.do").param("r", "res1")
        .param("v", version).param("summary", "Fixed the names").param("save", "Save"));
    assertThat(saved.getRedirectedUrl()).isEqualTo("/manage/detail.do?r=res1&v=" + version);
    assertThat(resources.get("res1").getVersionHistory().get(0).getChangeSummary()).isEqualTo("Fixed the names");

    MockHttpServletResponse detail = as(ManageFixture.OWNER, get("/manage/detail.do?r=res1"));
    assertThat(detail.getStatus()).isEqualTo(200);
    assertThat(detail.getContentAsString()).doesNotContain("FreeMarker template error");
    assertThat(as(ManageFixture.OTHER, get("/manage/detail.do?r=res1")).getStatus()).isEqualTo(401);
    assertThat(as(ManageFixture.OTHER, get("/manage/history.do?r=res1&v=" + version)).getStatus()).isEqualTo(401);
  }
}
