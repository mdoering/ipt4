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
import org.gbif.ipt.model.ExtensionMapping;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
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

/** IPT 3's MappingAction, TranslationAction and vocabulary popup. */
@SpringBootTest
@AutoConfigureMockMvc
class MappingTest {

  private static final String OCCURRENCE = "http://rs.tdwg.org/dwc/terms/Occurrence";
  private static final String ENCODED = URLEncoder.encode(OCCURRENCE, StandardCharsets.UTF_8);

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired org.gbif.ipt.service.admin.ExtensionManager extensions;

  @BeforeEach
  void owner() throws Exception {
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
  void theCoreMappingPage() throws Exception {
    MockHttpServletResponse r = as(ManageFixture.OWNER,
        get(URI.create("/manage/mapping.do?r=res1&id=" + ENCODED + "&mid=0")));
    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(r.getContentAsString()).contains("fields[").contains("scientificName")
        .doesNotContain("FreeMarker template error");
    assertThat(as(ManageFixture.OTHER, get(URI.create("/manage/mapping.do?r=res1&id=" + ENCODED + "&mid=0")))
        .getStatus()).isEqualTo(401);
  }

  /** "Use the resource DOI as datasetID" is saved when ticked and when unticked again. */
  @Test
  void datasetDoiCheckboxIsSaved() throws Exception {
    assertThat(saveMappingPage(true).isDoiUsedForDatasetId()).isTrue();
    assertThat(saveMappingPage(false).isDoiUsedForDatasetId()).isFalse();
  }

  private ExtensionMapping saveMappingPage(boolean doiUsedForDatasetId) throws Exception {
    String url = "/manage/mapping.do?r=res1&id=" + ENCODED + "&mid=0";
    org.jsoup.nodes.Document page = org.jsoup.Jsoup.parse(as(ManageFixture.OWNER, get(URI.create(url)))
        .getContentAsString(), "http://localhost" + url);
    org.jsoup.select.Elements box = page.select("input.datasetDoiCheckbox");
    assertThat(box).hasSize(1);
    if (doiUsedForDatasetId) {
      box.attr("checked", "checked");
    } else {
      box.removeAttr("checked");
    }
    MockHttpServletResponse saved = as(ManageFixture.OWNER,
        org.gbif.ipt.web.BrowserForms.submit(page, "mapping.do", null, "", java.util.Map.of()));
    assertThat(saved.getStatus()).as(saved.getContentAsString()).isLessThan(400);
    return resources.get("res1").getMappings(OCCURRENCE).get(0);
  }

  @Test
  void mapANewSourceAndDeleteTheMapping() throws Exception {
    as(ManageFixture.OWNER, multipart("/manage/addsource.do")
        .file(new MockMultipartFile("file", "extra.txt", "text/plain",
            "id\tscientificName\n9\tLynx lynx\n".getBytes(StandardCharsets.UTF_8)))
        .param("r", "res1").param("sourceType", "source-file").param("validate", "false"));
    int before = resources.get("res1").getMappings(OCCURRENCE).size();

    MockHttpServletResponse chooseSource = as(ManageFixture.OWNER,
        get(URI.create("/manage/mapping.do?r=res1&id=" + ENCODED)));
    assertThat(chooseSource.getStatus()).isEqualTo(200);
    assertThat(chooseSource.getContentAsString()).contains("extra");

    MockHttpServletResponse saved = as(ManageFixture.OWNER, post("/manage/mapping.do").param("r", "res1")
        .param("id", OCCURRENCE).param("source", "extra").param("save", "Save"));
    assertThat(saved.getRedirectedUrl()).isEqualTo("/manage/mapping.do?r=res1&id=" + ENCODED + "&mid=" + before);
    assertThat(resources.get("res1").getMappings(OCCURRENCE)).hasSize(before + 1);
    assertThat(resources.get("res1").getMappings(OCCURRENCE).get(before).getSource().getName()).isEqualTo("extra");

    MockHttpServletResponse deleted = as(ManageFixture.OWNER, post("/manage/delete-mapping.do").param("r", "res1")
        .param("id", OCCURRENCE).param("mid", String.valueOf(before)));
    assertThat(deleted.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=res1");
    assertThat(resources.get("res1").getMappings(OCCURRENCE)).hasSize(before);
  }

  @Test
  void translateValues() throws Exception {
    MockHttpSession session = new MockHttpSession();
    String url = "/manage/translation.do?r=res1&rowtype=" + ENCODED + "&mid=0&term="
        + URLEncoder.encode("http://rs.tdwg.org/dwc/terms/basisOfRecord", StandardCharsets.UTF_8);
    MockHttpServletResponse page = mvc.perform(get(URI.create(url)).session(session)
        .with(user(ManageFixture.OWNER))).andReturn().getResponse();
    assertThat(page.getStatus()).isEqualTo(200);
    assertThat(page.getContentAsString()).contains("tmap").doesNotContain("FreeMarker template error");

    MockHttpServletResponse saved = mvc.perform(post("/manage/translation.do").session(session)
        .with(user(ManageFixture.OWNER)).with(csrf()).param("r", "res1").param("rowtype", OCCURRENCE)
        .param("mid", "0").param("term", "http://rs.tdwg.org/dwc/terms/basisOfRecord")
        .param("tmap['occurrence']", "HumanObservation").param("save", "Save")).andReturn().getResponse();
    assertThat(saved.getRedirectedUrl()).isEqualTo("/manage/mapping.do?r=res1&id=" + ENCODED + "&mid=0");
    assertThat(resources.get("res1").getMappings(OCCURRENCE).get(0)
        .getField("http://rs.tdwg.org/dwc/terms/basisOfRecord").getTranslation())
        .containsValue("HumanObservation");
  }

  /** A mapping's field terms are the installed extension's properties, shared by every resource. */
  @Test
  void installedExtensionsCannotBeChangedThroughAMapping() throws Exception {
    for (String prefix : new String[]{"fields[0].term", "fields[1].term", "mapping.fields[0].term"}) {
      as(ManageFixture.OWNER, post("/manage/mapping.do").param("r", "res1").param("id", OCCURRENCE).param("mid", "0")
          .param(prefix + ".description", "HACKED").param(prefix + ".name", "hacked")
          .param(prefix + ".required", "true").param("save", "Save"));
    }
    assertThat(extensions.get(OCCURRENCE).getProperties())
        .noneMatch(p -> "HACKED".equals(p.getDescription()) || "hacked".equals(p.getName()));
  }

  @Test
  void vocabularyPopup() throws Exception {
    MockHttpServletResponse r = as(ManageFixture.OWNER, get(URI.create("/manage/vocabulary.do?id="
        + URLEncoder.encode("http://rs.tdwg.org/dwc/dwctype/", StandardCharsets.UTF_8))));
    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(r.getContentAsString()).contains("PreservedSpecimen");
  }
}
