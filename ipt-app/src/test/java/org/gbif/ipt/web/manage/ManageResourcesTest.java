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
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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

/** IPT 3's manage home, resource creation and manager API. */
@SpringBootTest
@AutoConfigureMockMvc
class ManageResourcesTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;

  @BeforeEach
  void managers() throws Exception {
    User owner = ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    ManageFixture.user(users, ManageFixture.OTHER, User.Role.Manager);
    Resource res1 = resources.get("res1");
    if (!res1.getManagers().contains(owner)) {
      res1.addManager(owner);
    }
  }

  private static MockMultipartFile upload(String fixture, String name) throws Exception {
    try (InputStream in = ManageResourcesTest.class.getResourceAsStream("/create/" + fixture)) {
      return new MockMultipartFile("file", name, "application/octet-stream", in.readAllBytes());
    }
  }

  private JsonNode json(String url, String as) throws Exception {
    MockHttpServletResponse r = mvc.perform(get(url).with(user(as))).andReturn().getResponse();
    assertThat(r.getStatus()).as(url).isEqualTo(200);
    assertThat(r.getContentType()).isEqualTo("application/json;charset=UTF-8");
    return JsonMapper.shared().readTree(r.getContentAsString());
  }

  @Test
  void homeListsTheManagersResources() throws Exception {
    for (String url : new String[]{"/manage/", "/manage", "/manage/home.do"}) {
      MockHttpServletResponse r = mvc.perform(get(url).with(user(ManageFixture.OWNER))).andReturn().getResponse();
      assertThat(r.getStatus()).as(url).isEqualTo(200);
      assertThat(r.getContentAsString()).as(url).contains("manager-api/resources");
    }
    assertThat(json("/manager-api/resources?start=0&length=10", ManageFixture.OWNER).toString()).contains("res1");
    JsonNode other = json("/manager-api/resources?start=0&length=10", ManageFixture.OTHER);
    assertThat(other.get("iTotalDisplayRecords").asInt()).isZero();
    assertThat(other.get("aaData")).isEmpty();
    assertThat(other.toString()).doesNotContain("res1");
  }

  @Test
  void createEmptyResource() throws Exception {
    MockHttpServletResponse r = mvc.perform(post("/manage/create.do").with(user(ManageFixture.OWNER)).with(csrf())
        .param("shortname", "empty1").param("resourceType", "occurrence")).andReturn().getResponse();
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=empty1");
    assertThat(resources.get("empty1").getCreator().getEmail()).isEqualTo(ManageFixture.OWNER);
  }

  @Test
  void createFromDarwinCoreArchive() throws Exception {
    MockHttpServletResponse r = mvc.perform(multipart("/manage/create.do").file(upload("dwca.zip", "dwca.zip"))
        .with(user(ManageFixture.OWNER)).with(csrf()).param("shortname", "dwca1").param("resourceType", "occurrence"))
        .andReturn().getResponse();
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=dwca1");
    Resource dwca1 = resources.get("dwca1");
    assertThat(dwca1.getSources()).isNotEmpty();
    assertThat(dwca1.getMappings()).isNotEmpty();
    assertThat(dwca1.getEml().getTitle()).startsWith("Aves Tanzanian collection");
  }

  @Test
  void createFromEml() throws Exception {
    MockHttpServletResponse r = mvc.perform(multipart("/manage/create.do").file(upload("eml.xml", "eml.xml"))
        .with(user(ManageFixture.OWNER)).with(csrf()).param("shortname", "eml1").param("resourceType", "metadata"))
        .andReturn().getResponse();
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=eml1");
    assertThat(resources.get("eml1").getEml().getTitle()).isEqualTo("WII Herbarium Dataset");
  }

  @Test
  void createFromDataPackageMetadata() throws Exception {
    MockHttpServletResponse r = mvc.perform(multipart("/manage/create.do")
            .file(upload("camtrap-datapackage.json", "datapackage.json")).with(user(ManageFixture.OWNER)).with(csrf())
            .param("shortname", "cam1").param("resourceType", "camtrap-dp"))
        .andReturn().getResponse();
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=cam1");
    Resource cam1 = resources.get("cam1");
    assertThat(cam1.isDataPackage()).isTrue();
    assertThat(cam1.getDataPackageMetadata().getTitle()).startsWith("Monitoreo de biodiversidad");
  }

  @Test
  void invalidOrTakenShortnamesShowTheForm() throws Exception {
    for (String shortname : new String[]{"a b", "x", "res1"}) {
      MockHttpServletResponse r = mvc.perform(post("/manage/create.do").with(user(ManageFixture.OWNER)).with(csrf())
          .param("shortname", shortname).param("resourceType", "occurrence")).andReturn().getResponse();
      assertThat(r.getStatus()).as(shortname).isEqualTo(200);
      assertThat(r.getContentAsString()).as(shortname).contains("invalid-feedback");
    }
    assertThat(mvc.perform(get("/manage/create.do").with(user(ManageFixture.OWNER))).andReturn().getResponse()
        .getStatus()).isEqualTo(200);
  }

  @Test
  void uploadedFileNamesCannotChooseWhereFilesGo() throws Exception {
    MockHttpServletResponse r = mvc.perform(multipart("/manage/create.do")
            .file(upload("eml.xml", "../../../evil-eml.xml")).with(user(ManageFixture.OWNER)).with(csrf())
            .param("shortname", "eml2").param("resourceType", "metadata"))
        .andReturn().getResponse();
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=eml2");
    try (Stream<Path> files = Files.walk(dataDir.getParent())) {
      assertThat(files.filter(f -> f.getFileName().toString().contains("evil"))).isEmpty();
    }
  }

  @Test
  void suggestions() throws Exception {
    assertThat(json("/manager-api/suggest-resources", ManageFixture.OWNER).has("res1")).isTrue();
    assertThat(json("/manager-api/suggest-agents?r=res1&type=contacts", ManageFixture.OWNER).has("suggestedAgents"))
        .isFalse();
    assertThat(mvc.perform(get("/manager-api/suggest-agents?r=res1&type=contacts").with(user(ManageFixture.OTHER)))
        .andReturn().getResponse().getStatus()).isEqualTo(401);
  }
}
