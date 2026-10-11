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
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.MetadataSection;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.web.BrowserForms;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's MetadataAction: every EML section, with IPT4's working copy. */
@SpringBootTest
@AutoConfigureMockMvc
class EmlEditorTest {

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
  void resourceFromAFullEml() throws Exception {
    ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    if (resources.get("eml1") == null) {
      try (InputStream in = getClass().getResourceAsStream("/create/eml.xml")) {
        mvc.perform(multipart("/manage/create.do").file(new MockMultipartFile("file", "eml.xml", "text/xml",
                in.readAllBytes())).param("shortname", "eml1").param("resourceType", "occurrence")
            .with(user(ManageFixture.OWNER)).with(csrf()));
      }
    }
    assertThat(resources.get("eml1")).isNotNull();
  }

  private MockHttpServletResponse as(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.with(user(ManageFixture.OWNER)).with(csrf())).andReturn().getResponse();
  }

  private MockHttpServletRequestBuilder submit(Document page, String section) {
    return submit(page, section, java.util.Map.of());
  }

  private MockHttpServletRequestBuilder submit(Document page, String section, java.util.Map<String, String> changed) {
    return BrowserForms.submit(page, "metadata-" + section, resources.get("eml1").getEml(), "eml", changed);
  }

  private MockHttpServletResponse saveUnchanged(String section) throws Exception {
    MockHttpServletResponse form = as(get("/manage/metadata-" + section + ".do?r=eml1"));
    assertThat(form.getStatus()).as(section).isEqualTo(200);
    assertThat(form.getContentAsString()).as(section).doesNotContain("FreeMarker template error");
    org.springframework.test.web.servlet.MvcResult result = mvc.perform(submit(
        Jsoup.parse(form.getContentAsString(), "http://localhost/manage/"), section)
        .with(user(ManageFixture.OWNER)).with(csrf())).andReturn();
    if (result.getResponse().getRedirectedUrl() == null && result.getModelAndView() != null) {
      throw new AssertionError(section + " was not saved: " + result.getModelAndView().getModel().get("fieldErrors")
          + " " + result.getModelAndView().getModel().get("actionErrors"));
    }
    return result.getResponse();
  }

  /**
   * The first save writes the imported EML in IPT's own form (EML 2.2, IPT package id), as IPT 3 does; from then on a
   * section saved unchanged leaves eml.xml byte-identical.
   */
  @Test
  void everySectionSurvivesAnUnchangedSave() throws Exception {
    Path eml = dataDir.resolve("resources/eml1/eml.xml");
    for (MetadataSection section : MetadataSection.values()) {
      saveUnchanged(section.getName());
    }
    for (MetadataSection section : MetadataSection.values()) {
      byte[] before = Files.readAllBytes(eml);
      MockHttpServletResponse saved = saveUnchanged(section.getName());
      assertThat(saved.getRedirectedUrl()).as(section.getName()).startsWith("/manage/metadata-");
      assertThat(new String(Files.readAllBytes(eml))).as(section.getName()).isEqualTo(new String(before));
    }
  }

  @Test
  void aRejectedSectionChangesNothing() throws Exception {
    String title = resources.get("eml1").getEml().getTitle();
    MockHttpServletResponse form = as(get("/manage/metadata-basic.do?r=eml1"));
    MockHttpServletRequestBuilder post = submit(Jsoup.parse(form.getContentAsString(), "http://localhost/manage/"),
        "basic", java.util.Map.of("eml.title", ""));
    MockHttpServletResponse rejected = as(post);
    assertThat(rejected.getStatus()).isEqualTo(200);
    assertThat(rejected.getContentAsString()).contains("invalid-feedback");
    assertThat(resources.get("eml1").getEml().getTitle()).isEqualTo(title);
  }

  @Test
  void logoUpload() throws Exception {
    byte[] png = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    MockHttpServletResponse uploaded = mvc.perform(multipart("/manage/uploadlogo.do")
        .file(new MockMultipartFile("file", "../../logo.png", "image/png", png)).param("r", "eml1")
        .with(user(ManageFixture.OWNER)).with(csrf())).andReturn().getResponse();
    assertThat(uploaded.getRedirectedUrl()).isEqualTo("/manage/metadata-additional.do?r=eml1");
    MockHttpServletResponse logo = mvc.perform(get("/logo.do?r=eml1").with(user(ManageFixture.OWNER))).andReturn()
        .getResponse();
    assertThat(logo.getContentType()).startsWith("image/png");
    assertThat(logo.getContentAsByteArray()).isEqualTo(png);

    org.springframework.test.web.servlet.MvcResult refused = mvc.perform(multipart("/manage/uploadlogo.do")
        .file(new MockMultipartFile("file", "logo.html", "text/html", "<script>x</script>".getBytes()))
        .param("r", "eml1").with(user(ManageFixture.OWNER)).with(csrf())).andReturn();
    assertThat(refused.getResponse().getRedirectedUrl()).isEqualTo("/manage/metadata-additional.do?r=eml1");
    assertThat(String.valueOf(refused.getFlashMap().get("actionErrors"))).contains("JPEG, GIF, PNG");
    try (java.util.stream.Stream<Path> files = Files.list(dataDir.resolve("resources/eml1"))) {
      assertThat(files.map(f -> f.getFileName().toString()).filter(n -> n.startsWith("logo")))
          .containsExactly("logo.png");
    }
  }

  @Test
  void unknownSectionsAreNotFound() throws Exception {
    assertThat(as(get("/manage/metadata-nothing.do?r=eml1")).getStatus()).isEqualTo(404);
  }
}
