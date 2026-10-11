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
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.ExtensionManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.nio.file.Path;
import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Updates an older installed Occurrence core (from rs.gbif.org) to the registry's latest version, keeping res1's
 * mappings. Reaches gbrds.gbif.org and rs.gbif.org: {@code mvn test -DexcludedGroups= -Dgroups=network}.
 */
@Tag("network")
@SpringBootTest
@AutoConfigureMockMvc
class RegistryExtensionsNetworkTest {

  private static final String ADMIN = "network.admin@example.org";
  private static final String OCCURRENCE = "http://rs.tdwg.org/dwc/terms/Occurrence";
  private static final String OLDER = "https://rs.gbif.org/core/dwc_occurrence_2024-02-23.xml";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    // an IPT with an older Occurrence core installed
    try (java.io.InputStream older = java.net.URI.create(OLDER).toURL().openStream()) {
      java.nio.file.Files.copy(older, dataDir.resolve("config/.extensions/http_rs_tdwg_org_dwc_terms_Occurrence.xml"),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    registry.add("ipt.data-dir", dataDir::toString);
    registry.add("ipt.defaults[dev.registry.url]", () -> "https://gbrds.gbif.org");
    registry.add("ipt.defaults[dev.registrydev.url]", () -> "https://gbrds.gbif.org");
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ExtensionManager extensions;
  @Autowired ResourceManager resources;

  @Test
  void updateAnOlderCore() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
    assertThat(day.format(extensions.get(OCCURRENCE).getIssued())).isEqualTo("2024-02-23");

    mvc.perform(post("/admin/updateExtension.do").param("id", OCCURRENCE).with(user(ADMIN)).with(csrf()));
    assertThat(extensions.get(OCCURRENCE).getIssued()).isAfter(day.parse("2024-02-23"));
    assertThat(resources.get("res1").getMappings(OCCURRENCE)).isNotEmpty()
        .allSatisfy(m -> assertThat(m.getExtension().getIssued()).isEqualTo(extensions.get(OCCURRENCE).getIssued()));
  }

  /** The installed schemas against the registry's: listed without registry errors. */
  @Test
  void schemasAgainstTheRegistry() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    String html = mvc.perform(get("/admin/dataPackages.do").with(user(ADMIN))).andReturn().getResponse()
        .getContentAsString();
    assertThat(html).contains("camtrap-dp").doesNotContain("Couldn't load registered data package")
        .doesNotContain("communicate with the GBIF Registry");
  }
}
