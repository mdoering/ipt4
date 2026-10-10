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
package org.gbif.ipt.app;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.feedback.CollectingFeedback;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.persistence.DataDirFormat;
import org.gbif.ipt.service.admin.ConfigManager;
import org.gbif.ipt.service.admin.ExtensionManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Boots the whole application on a small IPT4 data dir and checks the core services loaded it. */
@SpringBootTest
class CoreContextTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws IOException {
    dataDir = Files.createTempDirectory("ipt4-core-context");
    copy("datadir-fixture/users.json", "config/users.json");
    copy("datadir-fixture/ipt.properties", "config/ipt.properties");
    // locks the data dir to the GBIF test registry, as setup does
    Files.writeString(dataDir.resolve("config/.gbifreg"), "DEVELOPMENT");
    copy("datadir-fixture/dwc_occurrence.xml", "config/.extensions/dwc_occurrence.xml");
    copy("datadir-fixture/resource.json", "resources/res1/resource.json");
    copy("datadir-fixture/eml.xml", "resources/res1/eml.xml");
    copy("datadir-fixture/occurrence.txt", "resources/res1/sources/occurrence.txt");
    DataDirFormat.writeMarker(dataDir, "test", null);
    registry.add("ipt.data-dir", dataDir::toString);
  }

  private static void copy(String from, String to) throws IOException {
    Path target = dataDir.resolve(to);
    Files.createDirectories(target.getParent());
    try (InputStream in = CoreContextTest.class.getClassLoader().getResourceAsStream(from)) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Autowired ConfigManager configManager;
  @Autowired AppConfig cfg;
  @Autowired UserAccountManager users;
  @Autowired ExtensionManager extensions;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;

  @Test
  void loadsUsers() {
    User admin = users.get("admin@example.org");
    assertThat(admin).isNotNull();
    assertThat(admin.getRole()).isEqualTo(User.Role.Admin);
  }

  @Test
  void loadsInstalledExtensionsOffline() {
    assertThat(extensions.get(DwcTerm.Occurrence.qualifiedName())).isNotNull();
  }

  @Test
  void loadsResourcesLinkedToUsersAndExtensions() {
    Resource res1 = resources.get("res1");
    assertThat(res1).isNotNull();
    assertThat(res1.getCreator()).isSameAs(users.get("admin@example.org"));
    assertThat(res1.getMappings()).hasSize(1);
    assertThat(res1.getMappings().get(0).getExtension()).isSameAs(extensions.get(DwcTerm.Occurrence.qualifiedName()));
    assertThat(res1.getEml().getTitle()).isNotBlank();
  }

  @Test
  void createsResourcesFromIpt3ResourceFolders() throws Exception {
    Path zip = Files.createTempFile("res1-ipt3", ".zip");
    try (InputStream in = CoreContextTest.class.getClassLoader().getResourceAsStream("datadir-fixture/res1-ipt3.zip")) {
      Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
    }
    User admin = users.get("admin@example.org");
    Resource created = resources.create("fromipt3", null, zip.toFile(), admin, new CollectingFeedback(null, admin));
    assertThat(created.getMappings()).hasSize(1);
    assertThat(created.getMappings().get(0).getExtension()).isSameAs(extensions.get(DwcTerm.Occurrence.qualifiedName()));
    assertThat(dataDir.resolve("resources/fromipt3/resource.json")).exists();
    assertThat(dataDir.resolve("resources/fromipt3/resource.xml")).doesNotExist();
  }

  @Test
  void readsTheDataDirConfiguration() {
    assertThat(cfg.getBaseUrl()).isEqualTo("http://localhost:7001");
    assertThat(configManager.setupComplete()).isTrue();
  }
}
