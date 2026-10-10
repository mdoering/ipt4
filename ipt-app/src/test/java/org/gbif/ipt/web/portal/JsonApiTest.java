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
package org.gbif.ipt.web.portal;

import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The JSON IPT 3 served through the Struts JSON plugin. Key sets and value types are those of the captures taken from
 * an IPT 3 installation with tools/golden/capture.sh.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JsonApiTest {

  private static final UUID RES1_KEY = UUID.fromString("57dfbfb4-6213-4df5-aa5b-4adc3d213ebf");

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = PortalFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired UserAccountManager users;
  @Autowired RegistrationManager registrations;

  /** res1 published, then marked registered as the registry would have done. */
  @BeforeEach
  void publishAndRegister() throws Exception {
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    Resource res1 = resources.get("res1");
    res1.setKey(RES1_KEY);
    res1.setStatus(PublicationStatus.REGISTERED);
  }

  private JsonNode json(String url) throws Exception {
    MockHttpServletResponse r = mvc.perform(get(url)).andReturn().getResponse();
    assertThat(r.getStatus()).as(url).isEqualTo(200);
    assertThat(r.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(r.getHeader("Cache-Control")).isEqualTo("no-cache");
    assertThat(r.getHeader("Pragma")).isEqualTo("No-cache");
    assertThat(r.getHeader("Expires")).isNotNull();
    return JsonMapper.shared().readTree(r.getContentAsString());
  }

  private static List<String> keys(JsonNode node) {
    return new ArrayList<>(node.propertyNames());
  }

  @Test
  void inventoryV1() throws Exception {
    JsonNode root = json("/inventory/dataset");
    assertThat(keys(root)).containsExactly("registeredResources");
    JsonNode item = root.get("registeredResources").get(0);
    // Struts wrote bean properties in name order, nulls included
    assertThat(keys(item)).containsExactly("dwca", "eml", "gbifKey", "lastPublished", "records",
        "recordsByExtension", "title", "type", "version");
    assertThat(item.get("gbifKey").asString()).isEqualTo(RES1_KEY.toString());
    assertThat(item.get("lastPublished").asString()).matches("\\d{4}-\\d{2}-\\d{2}");
    assertThat(item.get("records").isInt()).isTrue();
    assertThat(item.get("version").isNumber()).isTrue();
    assertThat(item.get("dwca").asString()).endsWith("/archive.do?r=res1");
    assertThat(item.get("eml").asString()).endsWith("/eml.do?r=res1");
    assertThat(item.get("recordsByExtension").isObject()).isTrue();
    assertThat(item.get("type").asString()).isEqualTo("OCCURRENCE");
  }

  @Test
  void inventoryV2() throws Exception {
    JsonNode root = json("/inventory/v2/dataset");
    assertThat(keys(root)).containsExactly("resources");
    JsonNode item = root.get("resources").get(0);
    assertThat(keys(item)).containsExactly("additionalProperties", "archive", "format", "gbifKey", "id",
        "lastPublished", "metadata", "records", "title", "version");
    assertThat(item.get("id").asString()).isEqualTo("res1");
    assertThat(item.get("format").asString()).isEqualTo("DWCA");
    assertThat(keys(item.get("archive").get(0))).containsExactly("type", "url");
    assertThat(item.get("metadata").get(0).get("type").asString()).isEqualTo("EML");
    assertThat(Set.copyOf(keys(item.get("additionalProperties")))).isEqualTo(Set.of("core", "recordsByExtension"));
    assertThat(item.get("lastPublished").asString()).matches("\\d{4}-\\d{2}-\\d{2}");
    assertThat(json("/inventory/v2/dataset.do?type=dwca").get("resources")).hasSize(1);
    assertThat(json("/inventory/v2/dataset?type=coldp").get("resources")).isEmpty();
  }

  @Test
  void inventoryV2NeverListsPrivateResources() throws Exception {
    PortalFixture.publish("private1", dataDir, resources, publications, users, registrations);
    Resource private1 = resources.get("private1");
    assertThat(private1.getStatus()).isEqualTo(PublicationStatus.PRIVATE);
    assertThat(private1.getLastPublishedVersionsVersion()).isNotNull();
    for (String status : List.of("private", "PRIVATE", "deleted", "nonsense")) {
      assertThat(json("/inventory/v2/dataset?status=" + status).toString()).doesNotContain("private1", "gone1");
    }
    assertThat(json("/inventory/v2/dataset?status=registered").get("resources")).hasSize(1);
  }

  @Test
  void apiResources() throws Exception {
    JsonNode root = json("/api/resources?start=0&length=10");
    assertThat(keys(root)).containsExactly("aaData", "iTotalDisplayRecords", "iTotalRecords");
    assertThat(root.get("iTotalRecords").asInt()).isEqualTo(1);
    assertThat(root.get("aaData").get(0).isArray()).isTrue();
    assertThat(root.toString()).doesNotContain("private1");
  }

  @Test
  void apiResourcesRejectsNonNumericPaging() throws Exception {
    assertThat(mvc.perform(get("/api/resources?start=abc")).andReturn().getResponse().getStatus()).isEqualTo(400);
    assertThat(mvc.perform(get("/api/resources?order[0][column]=x")).andReturn().getResponse().getStatus())
        .isEqualTo(400);
  }

  @Test
  void apiHealth() throws Exception {
    JsonNode root = json("/api/health");
    assertThat(keys(root)).containsExactly("status");
    assertThat(keys(root.get("status"))).containsExactly("diskFree", "diskTotal", "diskUsed", "diskUsedRatio",
        "networkPublicAccess", "networkPublicAccessURL", "networkRegistry", "networkRegistryURL",
        "networkRepository", "networkRepositoryURL", "readConfigDir", "readLogDir", "readResourcesDir",
        "readSubResourcesDir", "readTmpDir", "writeLogDir", "writeResourcesDir", "writeSubResourcesDir",
        "writeTmpDir");
    assertThat(root.get("status").get("diskTotal").isNumber()).isTrue();
    assertThat(root.get("status").get("readConfigDir").asBoolean()).isTrue();
  }
}
