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
import org.gbif.ipt.model.Registration;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.persistence.DataDirFormat;
import org.gbif.ipt.persistence.JsonResourcePersister;
import org.gbif.ipt.persistence.ReferenceLookup;
import org.gbif.ipt.persistence.RegistrationStore;
import org.gbif.ipt.persistence.SecretCipher;
import org.gbif.ipt.persistence.UserStore;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigratorTest {

  @TempDir
  Path tmp;

  private Path from() throws Exception {
    return Ipt3Fixtures.dataDir(tmp.resolve("ipt3"));
  }

  private Resource loadJson(Path target, String shortname) throws Exception {
    SecretCipher cipher = SecretCipher.load(target.resolve("config"), Map.of());
    JsonResourcePersister persister = new JsonResourcePersister(cipher, JdbcSupport.fromClasspath(), new ReferenceLookup() {
      @Override
      public User user(String email) {
        User u = new User();
        u.setEmail(email);
        return u;
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
    });
    try (InputStream in = Files.newInputStream(target.resolve("resources/" + shortname + "/resource.json"))) {
      return persister.load(in);
    }
  }

  @Test
  void migratesAnIpt3DataDir() throws Exception {
    Path to = tmp.resolve("ipt4");
    MigrationReport report = new Migrator(Map.of()).migrate(from(), to, MigrationOptions.defaults());

    assertThat(report.errors()).isEmpty();
    assertThat(DataDirFormat.detect(to)).isEqualTo(DataDirFormat.Kind.V4);
    assertThat(DataDirFormat.readMarker(to).migratedFrom()).containsKey("path");
    assertThat(to.resolve("config/ipt.properties")).exists();
    assertThat(to.resolve("config/.gbifreg")).hasContent("DEVELOPMENT");
    assertThat(to.resolve("config/.extensions/occurrence.xml")).exists();
    assertThat(to.resolve("config/users.xml")).doesNotExist();
    assertThat(to.resolve("config/registration2.xml")).doesNotExist();
    assertThat(to.resolve("logs")).doesNotExist();
    assertThat(to.resolve("migration-report.json")).exists();
    assertThat(to.resolve("migration-report.txt")).content().contains("res1");

    List<User> users = new UserStore(to.resolve("config/users.json")).load();
    assertThat(users).extracting(User::getEmail).containsExactlyInAnyOrder("admin@gbif.org", "old@gbif.org");
    assertThat(report.usersRehashed()).isEqualTo(1);

    for (String r : List.of("res1", "amphibians", "event")) {
      assertThat(to.resolve("resources/" + r + "/resource.xml")).doesNotExist();
      assertThat(to.resolve("resources/" + r + "/eml.xml")).exists();
      Resource resource = loadJson(to, r);
      assertThat(resource.getMappings()).as(r).isNotEmpty();
    }
    assertThat(to.resolve("resources/res1/sources/occurrence.txt")).exists();
    assertThat(to.resolve("resources/res1/eml-1.1.xml")).exists();
    assertThat(report.resources()).hasSize(3);
  }

  @Test
  void warnsAboutResourcesWhoseExtensionsAreNotInstalled() throws Exception {
    MigrationReport report = new Migrator(Map.of()).migrate(from(), tmp.resolve("ipt4"), MigrationOptions.defaults());
    // only the Occurrence extension is installed in the fixture data dir
    assertThat(report.warnings())
        .anySatisfy(w -> assertThat(w).contains("event").contains("http://rs.tdwg.org/dwc/terms/Event"))
        .anySatisfy(w -> assertThat(w).contains("amphibians").contains("http://rs.gbif.org/terms/1.0/Image"))
        .noneSatisfy(w -> assertThat(w).startsWith("Resource res1"));
  }

  @Test
  void namesThePasswordsItCouldNotDecrypt() throws Exception {
    MigrationReport report = new Migrator(Map.of()).migrate(from(), tmp.resolve("ipt4"), MigrationOptions.defaults());
    // the fixture's hosting organisation password is not valid PBE output
    assertThat(report.warnings()).anySatisfy(w -> assertThat(w)
        .contains("Old Name Academy of Natural Sciences").contains("f9b67ad0-9c9b-11d9-b9db-b8a03c50a862"));
  }

  @Test
  void reencryptsSecretsWithTheNewKey() throws Exception {
    Path to = tmp.resolve("ipt4");
    new Migrator(Map.of()).migrate(from(), to, MigrationOptions.defaults());
    SecretCipher cipher = SecretCipher.load(to.resolve("config"), Map.of());
    String json = Files.readString(to.resolve("config/registration.json"));
    assertThat(json).contains("{aesgcm}").doesNotContain("PsQfBTu3GJfy00mWe2MyOg");
    Registration reg = new RegistrationStore(to.resolve("config/registration.json"), cipher).load();
    Registration legacy = new LegacyXmlReader().readRegistration(
        getClass().getResourceAsStream("/ipt3/config/registration2.xml"));
    // ZMD's passwords are valid PBE values (the others in the fixture are not, see namesThePasswordsItCouldNotDecrypt)
    String zmd = "456058db-f70b-4005-97ad-e08570cf0c56";
    assertThat(legacy.getAssociatedOrganisations().get(zmd).getPassword()).isNotBlank();
    assertThat(reg.getAssociatedOrganisations().get(zmd).getPassword())
        .isEqualTo(legacy.getAssociatedOrganisations().get(zmd).getPassword());
    assertThat(reg.getAssociatedOrganisations()).hasSameSizeAs(legacy.getAssociatedOrganisations());
  }

  @Test
  void convertsVersion1Registrations() throws Exception {
    Path from = from();
    Files.delete(from.resolve("config/registration2.xml"));
    Ipt3Fixtures.copy("config/registration.xml", from.resolve("config/registration.xml"));
    Path to = tmp.resolve("ipt4");
    new Migrator(Map.of()).migrate(from, to, MigrationOptions.defaults());
    SecretCipher cipher = SecretCipher.load(to.resolve("config"), Map.of());
    Registration reg = new RegistrationStore(to.resolve("config/registration.json"), cipher).load();
    assertThat(reg.getIpt().getWsPassword()).isEqualTo("password");
    assertThat(to.resolve("config/registration.xml")).doesNotExist();
  }

  @Test
  void refusesNonEmptyTarget() throws Exception {
    Path to = tmp.resolve("ipt4");
    Files.createDirectories(to);
    Files.writeString(to.resolve("something"), "x");
    Path from = from();
    assertThatThrownBy(() -> new Migrator(Map.of()).migrate(from, to, MigrationOptions.defaults()))
        .isInstanceOf(MigrationException.class).hasMessageContaining("not empty");
  }

  @Test
  void refusesSourcesThatAreNoIpt3DataDir() throws Exception {
    Path from = tmp.resolve("random");
    Files.createDirectories(from);
    Files.writeString(from.resolve("x.txt"), "x");
    assertThatThrownBy(() -> new Migrator(Map.of()).migrate(from, tmp.resolve("ipt4"), MigrationOptions.defaults()))
        .isInstanceOf(MigrationException.class).hasMessageContaining("IPT 3");
  }

  @Test
  void dryRunWritesNothing() throws Exception {
    Path to = tmp.resolve("ipt4");
    MigrationReport report = new Migrator(Map.of()).migrate(from(), to, new MigrationOptions(true, false, false));
    assertThat(report.errors()).isEmpty();
    assertThat(report.resources()).hasSize(3);
    assertThat(to).doesNotExist();
  }

  @Test
  void canLinkVersionedArtifactsAndCopyLogs() throws Exception {
    Path from = from();
    Path to = tmp.resolve("ipt4");
    new Migrator(Map.of()).migrate(from, to, new MigrationOptions(false, true, true));
    assertThat(Files.isSameFile(from.resolve("resources/res1/eml-1.1.xml"), to.resolve("resources/res1/eml-1.1.xml")))
        .isTrue();
    assertThat(Files.isSameFile(from.resolve("resources/res1/eml.xml"), to.resolve("resources/res1/eml.xml")))
        .as("the current EML changes, so it is copied").isFalse();
    assertThat(to.resolve("logs/debug.log")).exists();
  }

  @Test
  void leavesTheSourceUntouched() throws Exception {
    Path from = from();
    long before;
    try (var files = Files.walk(from)) {
      before = files.count();
    }
    new Migrator(Map.of()).migrate(from, tmp.resolve("ipt4"), MigrationOptions.defaults());
    try (var files = Files.walk(from)) {
      assertThat(files.count()).isEqualTo(before);
    }
    assertThat(from.resolve("config/users.xml")).exists();
  }
}
