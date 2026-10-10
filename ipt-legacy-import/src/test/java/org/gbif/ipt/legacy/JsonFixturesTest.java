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
package org.gbif.ipt.legacy;

import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.persistence.JsonResourcePersister;
import org.gbif.ipt.persistence.ReferenceLookup;
import org.gbif.ipt.persistence.SecretCipher;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The importer turns every legacy resource.xml fixture into the resource.json fixture that ipt-core's tests use. Those
 * were converted once through IPT 3's own XStream setup with real extensions (see git history of ipt-core), so this
 * pins the importer to IPT 3's reading of the files.
 */
class JsonFixturesTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final ReferenceLookup keys = new ReferenceLookup() {
    @Override
    public User user(String email) {
      return null;
    }

    @Override
    public Organisation organisation(String key) {
      return null;
    }

    @Override
    public Extension extension(String rowType) {
      return null;
    }

    @Override
    public DataPackageSchema dataPackageSchema(String identifier) {
      return null;
    }
  };

  @ParameterizedTest
  @ValueSource(strings = {"res1/resource", "res1/resource_auto_ids", "res1/resource_doi_dataset_id",
      "res1/resource_event_1", "res1/resource_multivalue", "res1/resource_no_bor_mapped", "res1/resource_no_id_mapped",
      "res1/resource_nonexistent_ext", "res1/resource_reg_version_history", "res1/resource_v1_1",
      "res1/resource_version_history", "amphibians/resource", "event/resource"})
  void importGivesTheJsonFixture(String fixture) throws Exception {
    Resource resource;
    try (InputStream in = getClass().getResourceAsStream("/ipt3/resources/" + fixture + ".xml")) {
      resource = new LegacyXmlReader().readResource(in);
    }
    StringWriter json = new StringWriter();
    new JsonResourcePersister(new SecretCipher(new byte[32]), JdbcSupport.fromClasspath(), keys).save(resource, json);

    String expected;
    try (InputStream in = getClass().getResourceAsStream("/resources/" + fixture + ".json")) {
      expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(normalise(MAPPER.readTree(json.toString()))).isEqualTo(normalise(MAPPER.readTree(expected)));
  }

  /** Field order inside a mapping follows hash order in IPT 3 and term order in IPT4; secrets get a random IV. */
  private static JsonNode normalise(JsonNode node) {
    if (node instanceof ObjectNode o) {
      for (String name : new ArrayList<>(o.propertyNames())) {
        JsonNode child = o.get(name);
        if (child.isString() && child.asString().startsWith(SecretCipher.PREFIX)) {
          o.put(name, SecretCipher.PREFIX);
        } else {
          normalise(child);
        }
      }
      if (o.get("fields") instanceof ArrayNode fields) {
        List<JsonNode> sorted = new ArrayList<>();
        fields.forEach(sorted::add);
        sorted.sort(Comparator.comparing(JsonNode::toString));
        fields.removeAll();
        sorted.forEach(fields::add);
      }
    } else if (node instanceof ArrayNode a) {
      a.forEach(JsonFixturesTest::normalise);
    }
    return node;
  }
}
