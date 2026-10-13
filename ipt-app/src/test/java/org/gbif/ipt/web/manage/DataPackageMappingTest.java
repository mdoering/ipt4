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
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;
import org.gbif.ipt.model.User;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

/** IPT 3's data package mapping, from a new DwC-DP resource to a published Darwin Core Data Package. */
@SpringBootTest
@AutoConfigureMockMvc
class DataPackageMappingTest {

  private static final String DWC_DP = "http://rs.tdwg.org/dwc/dwc-dp";

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

  private MockHttpServletResponse as(AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
    return mvc.perform(request.with(user(ManageFixture.OWNER)).with(csrf())).andReturn().getResponse();
  }

  @Test
  void mapAndPublishADarwinCoreDataPackage() throws Exception {
    ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    assertThat(as(post("/manage/create.do").param("shortname", "dp1").param("resourceType", "dwc-dp"))
        .getRedirectedUrl()).isEqualTo("/manage/resource.do?r=dp1");
    assertThat(resources.get("dp1").getDataPackageIdentifier()).isEqualTo(DWC_DP);

    as(multipart("/manage/addsource.do").file(new MockMultipartFile("file", "events.txt", "text/plain",
            "event_pk\teventCategory\teventDate\ne1\tSurvey\t2020-01-01\ne2\tSurvey\t2020-02-01\n"
                .getBytes(StandardCharsets.UTF_8)))
        .param("r", "dp1").param("sourceType", "source-file").param("validate", "false"));
    assertThat(resources.get("dp1").getSource("events")).isNotNull();

    String schema = URLEncoder.encode(DWC_DP, StandardCharsets.UTF_8);
    MockHttpServletResponse selector = as(get(URI.create("/manage/dataPackageMappingSourceNew.do?r=dp1&id="
        + schema)));
    assertThat(selector.getStatus()).isEqualTo(200);
    assertThat(selector.getContentAsString()).contains("events");

    MockHttpServletResponse created = as(post("/manage/dataPackageMappingSourceCreate.do").param("r", "dp1")
        .param("id", DWC_DP).param("newSources[0]", "events").param("newTableSchemas[0]", "event"));
    assertThat(created.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=dp1");
    Resource dp1 = resources.get("dp1");
    assertThat(dp1.getDataPackageMappings()).hasSize(1);
    // automapped by column name (IPT 3's create() leaves fieldsMapped to the first save)
    assertThat(dp1.getDataPackageMappings().get(0).getFields())
        .anyMatch(f -> "event_pk".equals(f.getField().getName()) && Integer.valueOf(0).equals(f.getIndex()));

    MockHttpServletResponse mapping = as(get(URI.create("/manage/dataPackageMapping.do?r=dp1&id=" + schema
        + "&mid=0")));
    assertThat(mapping.getStatus()).isEqualTo(200);
    assertThat(mapping.getContentAsString()).contains("event_pk").doesNotContain("FreeMarker template error");

    // the metadata editors set the licence; IPT 3 refuses to publish a data package without one
    resources.get("dp1").getEml().setIntellectualRights("This work is licensed under a Creative Commons Attribution "
        + "(CC-BY 4.0) License.");
    as(post("/manage/publish.do").param("r", "dp1").param("publish", "Publish"));
    long deadline = System.currentTimeMillis() + 120_000;
    while (publications.isLocked("dp1") && System.currentTimeMillis() < deadline) {
      Thread.sleep(100);
    }
    MockHttpServletResponse archive = mvc.perform(get("/archive.do?r=dp1")).andReturn().getResponse();
    // a new resource is private: its manager downloads it
    MockHttpServletResponse ownArchive = as(get("/archive.do?r=dp1"));
    assertThat(ownArchive.getStatus()).isEqualTo(200);
    assertThat(archive.getStatus()).isEqualTo(401);
    List<String> entries = new ArrayList<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(ownArchive.getContentAsByteArray()))) {
      for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
        entries.add(e.getName());
      }
    }
    assertThat(entries).contains("datapackage.json").anyMatch(n -> n.startsWith("event"));
    assertThat(resources.get("dp1").getLastPublished()).isNotNull();

    assertThat(as(get("/manage/validationReport.do?r=dp1")).getStatus()).isEqualTo(200);

    MockHttpServletResponse deleted = as(post("/manage/deleteDataPackageMapping.do").param("r", "dp1")
        .param("id", DWC_DP).param("mid", "0"));
    assertThat(deleted.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=dp1");
    assertThat(resources.get("dp1").getDataPackageMappings()).isEmpty();
  }
}
