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

import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Registration;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.SqlSource;
import org.gbif.ipt.model.User;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyXmlReaderTest {

  private final LegacyXmlReader reader = new LegacyXmlReader();

  private InputStream fixture(String name) {
    return getClass().getResourceAsStream("/ipt3/" + name);
  }

  @ParameterizedTest
  @ValueSource(strings = {"res1/resource.xml", "res1/resource_auto_ids.xml", "res1/resource_doi_dataset_id.xml",
      "res1/resource_event_1.xml", "res1/resource_multivalue.xml", "res1/resource_no_bor_mapped.xml",
      "res1/resource_no_id_mapped.xml", "res1/resource_nonexistent_ext.xml", "res1/resource_reg_version_history.xml",
      "res1/resource_v1_1.xml", "res1/resource_version_history.xml", "amphibians/resource.xml", "event/resource.xml"})
  void readsEveryLegacyResourceFixture(String fixture) throws Exception {
    Resource r = reader.readResource(fixture("resources/" + fixture));
    assertThat(r.getCreator()).isNotNull();
    // every fixture has mappings, except the registered-version-history one, which has versions
    assertThat(r.getMappings().size() + r.getVersionHistory().size()).isPositive();
  }

  @Test
  void keepsReferencesAsKeys() throws Exception {
    Resource r = reader.readResource(fixture("resources/res1/resource.xml"));
    assertThat(r.getCreator().getEmail()).isEqualTo("kbraak@gbif.org");
    assertThat(r.getManagers()).extracting(User::getEmail).containsExactly("jcuadra@gbif.org");
    assertThat(r.getOrganisation().getKey()).hasToString("f3e8e9a9-df60-40ca-bb71-7f49313b3150");
    assertThat(r.getMappings().get(0).getExtension().getRowType()).isEqualTo("http://rs.tdwg.org/dwc/terms/Occurrence");
    assertThat(r.getMappings().get(0).getField("http://rs.tdwg.org/dwc/terms/basisOfRecord")).isNotNull();
    assertThat(r.getMappings().get(0).getSource()).isSameAs(r.getSource("occurrence"));
  }

  @Test
  void decryptsSqlSourcePasswords() throws Exception {
    String xml = """
        <resource>
          <sources>
            <sqlsource>
              <name>db</name>
              <rdbms>pgsql</rdbms>
              <password>%s</password>
            </sqlsource>
          </sources>
        </resource>
        """.formatted(Ipt3Fixtures.pbe("db-secret"));
    Resource r = reader.readResource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    SqlSource sql = (SqlSource) r.getSource("db");
    assertThat(sql.getPassword()).isEqualTo("db-secret");
    assertThat(sql.getRdbms().getName()).isEqualTo("pgsql");
  }

  @Test
  void readsUsersAndRehashesPbePasswords() throws Exception {
    String xml = """
        <users>
          <user email="admin@gbif.org" password="%s" role="Admin" lastLogin="2020-01-02 10:11:12.0 UTC"/>
          <user email="old@gbif.org" password="%s" role="Manager"/>
        </users>
        """.formatted(Ipt3Fixtures.ADMIN_HASH, Ipt3Fixtures.pbe("secret"));
    LegacyXmlReader.Users users = reader.readUsers(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    List<User> list = users.users();
    assertThat(list).extracting(User::getEmail).containsExactly("admin@gbif.org", "old@gbif.org");
    assertThat(list.get(0).getPassword()).isEqualTo(Ipt3Fixtures.ADMIN_HASH);
    assertThat(list.get(0).getLastLogin()).isNotNull();
    assertThat(new BCryptPasswordEncoder().matches("secret", list.get(1).getPassword())).isTrue();
    assertThat(users.rehashed()).isEqualTo(1);
  }

  @Test
  void readsEncryptedRegistrations() throws Exception {
    Registration reg = reader.readRegistration(fixture("config/registration2.xml"));
    assertThat(reg.getHostingOrganisation().getKey()).hasToString("f9b67ad0-9c9b-11d9-b9db-b8a03c50a862");
    assertThat(reg.getIpt().getName()).isEqualTo("Test IPT 15 Sept 2012");
    assertThat(reg.getAssociatedOrganisations()).hasSize(3);
    Organisation zmd = reg.getAssociatedOrganisations().get("456058db-f70b-4005-97ad-e08570cf0c56");
    assertThat(zmd.getAgencyAccountUsername()).isEqualTo("DK.KYLE");
    assertThat(zmd.getPassword()).isNotBlank();
  }

  @Test
  void readsVersion1Registrations() throws Exception {
    Registration reg = reader.readLegacyRegistration(fixture("config/registration.xml"));
    assertThat(reg.getHostingOrganisation().getKey()).hasToString("f9b67ad0-9c9b-11d9-b9db-b8a03c50a862");
    assertThat(reg.getHostingOrganisation().getPassword()).isEqualTo("password");
    assertThat(reg.getIpt().getWsPassword()).isEqualTo("password");
    assertThat(reg.getAssociatedOrganisations()).isNotEmpty();
  }
}
