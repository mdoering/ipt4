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
import org.gbif.ipt.service.admin.DataPackageSchemaManager;
import org.gbif.ipt.service.admin.ExtensionManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.admin.VocabulariesManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.net.URI;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
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

/** IPT 3's ExtensionsAction, VocabulariesAction and DataPackageSchemaAction, against a stub registry. */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminExtensionsTest {

  private static final String ADMIN = "extensions.admin@example.org";
  private static final String OCCURRENCE = "http://rs.tdwg.org/dwc/terms/Occurrence";
  private static final String IDENTIFIER = "http://rs.gbif.org/terms/1.0/Identifier";

  static Path dataDir;
  static StubRegistry stub;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
    stub = StubRegistry.start(registry);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ExtensionManager extensions;
  @Autowired VocabulariesManager vocabularies;
  @Autowired DataPackageSchemaManager schemas;
  @Autowired ResourceManager resources;

  @BeforeEach
  void admin() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
  }

  private MockHttpServletResponse get200(String url) throws Exception {
    MockHttpServletResponse response = mvc.perform(get(URI.create(url)).with(user(ADMIN))).andReturn().getResponse();
    assertThat(response.getStatus()).as(url).isEqualTo(200);
    assertThat(response.getContentAsString()).as(url).doesNotContain("FreeMarker template error");
    return response;
  }

  private MockHttpServletResponse send(String url, String... params) throws Exception {
    var request = post(URI.create(url)).with(user(ADMIN)).with(csrf());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  @Test
  void listShowsInstalledAndRegisteredExtensions() throws Exception {
    String html = get200("/admin/extensions.do").getContentAsString();
    assertThat(html).contains(OCCURRENCE);
    // the Identifier extension is registered, not installed
    assertThat(html).contains(stub.url() + "/rs/extension/gbif/1.0/identifier.xml");
  }

  @Test
  void installAndRemoveAnExtension() throws Exception {
    Element install = Jsoup.parse(get200("/admin/extensions.do").getContentAsString())
        .selectFirst("form[action=extension.do]:has(input[name=url][value*=identifier.xml])");
    assertThat(install).isNotNull();
    MockHttpServletResponse installed = send("/admin/extension.do", "url",
        install.selectFirst("input[name=url]").attr("value"), "install", "Install");
    assertThat(installed.getRedirectedUrl()).isEqualTo("/admin/extensions.do");
    assertThat(extensions.get(IDENTIFIER)).isNotNull();
    get200("/admin/extension.do?id=" + IDENTIFIER);

    // the list's remove button
    Element remove = Jsoup.parse(get200("/admin/extensions.do").getContentAsString())
        .selectFirst("form[action=extension.do]:has(input[name=id][value=" + IDENTIFIER + "])");
    assertThat(remove.selectFirst("button[type=submit]").attr("name")).isEqualTo("deleteFlag");
    send("/admin/extension.do", "id", IDENTIFIER, "deleteFlag", "Delete");
    assertThat(extensions.get(IDENTIFIER)).isNull();
  }

  @Test
  void installNeedsAWebLink() throws Exception {
    int before = extensions.list().size();
    send("/admin/extension.do", "url", "file:///etc/hosts", "install", "Install");
    assertThat(extensions.list()).hasSize(before);
    assertThat(stub.requests()).noneMatch(r -> r.path().contains("hosts"));
  }

  @Test
  void extensionInUseIsKept() throws Exception {
    MockHttpServletResponse refused = send("/admin/extension.do", "id", OCCURRENCE, "deleteFlag", "Delete");
    assertThat(refused.getRedirectedUrl()).isEqualTo("/admin/extensions.do");
    assertThat(extensions.get(OCCURRENCE)).isNotNull();
    assertThat(resources.get("res1").getMappings(OCCURRENCE)).isNotEmpty();
  }

  /** First: synchronising also brings installed extensions up to date. */
  @Test
  @Order(1)
  void updateKeepsMappings() throws Exception {
    int mapped = resources.get("res1").getMappings(OCCURRENCE).size();
    assertThat(mapped).isPositive();
    // the registry's newer Occurrence core is offered
    assertThat(Jsoup.parse(get200("/admin/extensions.do").getContentAsString())
        .select("form[action=updateExtension.do] input[name=id]")).extracting(e -> e.attr("value")).contains(OCCURRENCE);
    send("/admin/updateExtension.do", "id", OCCURRENCE, "update", "Update");
    SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
    assertThat(day.format(extensions.get(OCCURRENCE).getIssued())).isEqualTo("2026-09-01");
    assertThat(resources.get("res1").getMappings(OCCURRENCE)).hasSize(mapped)
        .allSatisfy(m -> assertThat(day.format(m.getExtension().getIssued())).isEqualTo("2026-09-01"));
    assertThat(Jsoup.parse(get200("/admin/extensions.do").getContentAsString())
        .select("form[action=updateExtension.do] input[name=id]")).extracting(e -> e.attr("value"))
        .doesNotContain(OCCURRENCE);
  }

  /** IPT 3 synchronised on a GET with ?synchronise=true; the page's button posts. */
  @Test
  void synchroniseOnlyOnPost() throws Exception {
    int before = stub.requests().size();
    get200("/admin/extensions.do?synchronise=true");
    assertThat(stub.requests().subList(before, stub.requests().size())).noneMatch(r -> r.path().startsWith("/rs/"));
    MockHttpServletResponse synced = send("/admin/extensions.do", "synchronise", "true");
    assertThat(synced.getStatus()).isEqualTo(200);
    assertThat(stub.requests().subList(before, stub.requests().size())).anyMatch(r -> r.path().startsWith("/rs/"));
  }

  @Test
  void vocabularyPage() throws Exception {
    String uri = vocabularies.list().get(0).getUriString();
    assertThat(get200("/admin/vocabulary.do?id=" + uri).getContentAsString())
        .contains(vocabularies.list().get(0).getTitle());
  }

  @Test
  void dataPackageSchemaPages() throws Exception {
    String camtrap = schemas.list().stream().filter(s -> "camtrap-dp".equals(s.getName())).findFirst().orElseThrow()
        .getIdentifier();
    assertThat(get200("/admin/dataPackages.do").getContentAsString())
        .contains("dataPackage.do?id=" + java.net.URLEncoder.encode(camtrap, java.nio.charset.StandardCharsets.UTF_8));
    get200("/admin/dataPackage.do?id=" + camtrap);
    assertThat(mvc.perform(get("/admin/dataPackage.do?id=http://example.org/none").with(user(ADMIN))).andReturn()
        .getResponse().getStatus()).isEqualTo(404);
  }
}
