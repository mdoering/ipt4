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
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.datapackage.metadata.FrictionlessMetadata;
import org.gbif.ipt.model.voc.CamtrapMetadataSection;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.web.BrowserForms;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's CamtrapMetadataAction and DataPackageMetadataAction (ColDP), on working copies. */
@SpringBootTest
@AutoConfigureMockMvc
class DataPackageMetadataEditorTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired DataDir dir;

  private void create(String shortname, String type, String fixture, String fileName) throws Exception {
    if (resources.get(shortname) == null) {
      try (InputStream in = getClass().getResourceAsStream("/create/" + fixture)) {
        mvc.perform(multipart("/manage/create.do").file(new MockMultipartFile("file", fileName, "text/plain",
                in.readAllBytes())).param("shortname", shortname).param("resourceType", type)
            .with(user(ManageFixture.OWNER)).with(csrf()));
      }
    }
    assertThat(resources.get(shortname)).as(shortname).isNotNull();
  }

  @BeforeEach
  void resources() throws Exception {
    ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    create("cam1", "camtrap-dp", "camtrap-datapackage.json", "datapackage.json");
    create("col1", "coldp", "coldp-metadata.yaml", "metadata.yaml");
  }

  private MvcResult saveUnchanged(String shortname, String path, Map<String, String> changed) throws Exception {
    MockHttpServletResponse form = mvc.perform(get("/manage/" + path + ".do?r=" + shortname)
        .with(user(ManageFixture.OWNER))).andReturn().getResponse();
    assertThat(form.getStatus()).as(path).isEqualTo(200);
    assertThat(form.getContentAsString()).as(path).doesNotContain("FreeMarker template error");
    MockHttpServletRequestBuilder post = BrowserForms.submit(Jsoup.parse(form.getContentAsString(),
        "http://localhost/manage/"), path, resources.get(shortname).getDataPackageMetadata(), "metadata", changed);
    return mvc.perform(post.with(user(ManageFixture.OWNER)).with(csrf())).andReturn();
  }

  private static void assertSaved(MvcResult result, String path) {
    if (result.getResponse().getRedirectedUrl() == null && result.getModelAndView() != null) {
      throw new AssertionError(path + " was not saved: " + result.getModelAndView().getModel().get("fieldErrors")
          + " " + result.getModelAndView().getModel().get("actionErrors"));
    }
  }

  @Test
  void camtrapSectionsSurviveAnUnchangedSave() throws Exception {
    Path file = dir.resourceDatapackageMetadataFile("cam1", "camtrap-dp").toPath();
    for (CamtrapMetadataSection section : CamtrapMetadataSection.values()) {
      assertSaved(saveUnchanged("cam1", "camtrap-metadata-" + section.getName(), Map.of()), section.getName());
    }
    for (CamtrapMetadataSection section : CamtrapMetadataSection.values()) {
      String before = Files.readString(file);
      assertSaved(saveUnchanged("cam1", "camtrap-metadata-" + section.getName(), Map.of()), section.getName());
      assertThat(Files.readString(file)).as(section.getName()).isEqualTo(before);
    }
  }

  @Test
  void aRejectedCamtrapSectionChangesNothing() throws Exception {
    String title = ((FrictionlessMetadata<?, ?, ?>) resources.get("cam1").getDataPackageMetadata()).getTitle();
    MvcResult rejected = saveUnchanged("cam1", "camtrap-metadata-basic", Map.of("metadata.title", ""));
    assertThat(rejected.getResponse().getRedirectedUrl()).isNull();
    assertThat(((FrictionlessMetadata<?, ?, ?>) resources.get("cam1").getDataPackageMetadata()).getTitle())
        .isEqualTo(title);
  }

  /** ColDP metadata is read-only on the overview (replaced by upload); IPT 3's editor URL failed with a 500. */
  @Test
  void colDpMetadataHasNoEditor() throws Exception {
    Path file = dir.resourceDatapackageMetadataFile("col1", "coldp").toPath();
    String before = Files.readString(file);
    assertThat(mvc.perform(get("/manage/datapackage-metadata-basic.do?r=col1").with(user(ManageFixture.OWNER)))
        .andReturn().getResponse().getStatus()).isEqualTo(404);
    assertThat(mvc.perform(post("/manage/datapackage-metadata-basic.do?r=col1").param("metadata.title", "changed")
        .with(user(ManageFixture.OWNER)).with(csrf())).andReturn().getResponse().getStatus()).isEqualTo(404);
    assertThat(resources.get("col1").getDataPackageMetadata().getTitle()).isNotEqualTo("changed");
    assertThat(Files.readString(file)).isEqualTo(before);
  }
}
