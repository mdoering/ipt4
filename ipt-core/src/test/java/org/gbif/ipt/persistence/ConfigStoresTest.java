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
package org.gbif.ipt.persistence;

import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Registration;
import org.gbif.ipt.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigStoresTest {

  @TempDir
  Path config;

  final SecretCipher cipher = new SecretCipher(new byte[32]);

  @Test
  void usersRoundTripWithTheirHashes() throws IOException {
    UserStore store = new UserStore(config.resolve("users.json"));
    assertThat(store.load()).isEmpty();

    User admin = new User();
    admin.setEmail("admin@example.org");
    admin.setPassword("$2a$12$abcdefghijklmnopqrstuuWb0V4o6M5wF3bC7m7FvKX9i3uXlG3l2");
    admin.setRole(User.Role.Admin);
    admin.setFirstname("Ada");
    admin.setLastLogin(new Date(1345123516892L));
    store.save(List.of(admin));

    List<User> loaded = store.load();
    assertThat(loaded).hasSize(1);
    assertThat(loaded.get(0).getEmail()).isEqualTo("admin@example.org");
    assertThat(loaded.get(0).getPassword()).isEqualTo(admin.getPassword());
    assertThat(loaded.get(0).getRole()).isEqualTo(User.Role.Admin);
    assertThat(loaded.get(0).getLastLogin()).isEqualTo(admin.getLastLogin());
  }

  @Test
  void registrationRoundTripsWithEncryptedPasswords() throws IOException {
    RegistrationStore store = new RegistrationStore(config.resolve("registration.json"), cipher);
    assertThat(store.load()).isNull();

    Organisation host = new Organisation();
    host.setKey("f3e8e9a9-df60-40ca-bb71-7f49313b3150");
    host.setName("GBIF");
    host.setPassword("org-secret");
    host.setCanHost(true);
    Ipt ipt = new Ipt();
    ipt.setKey("a3e8e9a9-df60-40ca-bb71-7f49313b3151");
    ipt.setName("Test IPT");
    ipt.setWsPassword("ipt-secret");
    Registration reg = new Registration();
    reg.setHostingOrganisation(host);
    reg.setIpt(ipt);
    reg.getAssociatedOrganisations().put(host.getKey().toString(), host);
    store.save(reg);

    String json = Files.readString(config.resolve("registration.json"));
    assertThat(json).doesNotContain("org-secret").doesNotContain("ipt-secret").contains("{aesgcm}");

    Registration loaded = store.load();
    assertThat(loaded.getHostingOrganisation().getPassword()).isEqualTo("org-secret");
    assertThat(loaded.getIpt().getWsPassword()).isEqualTo("ipt-secret");
    assertThat(loaded.getAssociatedOrganisations()).containsKey(host.getKey().toString());
    assertThat(loaded.getAssociatedOrganisations().get(host.getKey().toString()).isCanHost()).isTrue();
  }
}
