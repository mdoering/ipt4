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

import org.gbif.ipt.legacy.MigrationOptions;
import org.gbif.ipt.legacy.Migrator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

/**
 * An IPT4 data dir migrated from the {@code ipt3-datadir} test fixture: resource {@code res1} (registered, not yet
 * published by IPT4), {@code private1} (private) and {@code gone1} (deleted), and the admin kbraak@gbif.org.
 */
public final class PortalFixture {

  public static final String ADMIN = "kbraak@gbif.org";

  private PortalFixture() {
  }

  public static Path create() throws Exception {
    Path root = Files.createTempDirectory("ipt-portal");
    Path ipt3 = root.resolve("ipt3");
    for (String f : List.of("config/ipt.properties", "config/users.xml", "config/.gbifreg",
        "config/.extensions/dwc_occurrence.xml", "config/.vocabularies/http_iso_org_639-2.vocab",
        "config/.vocabularies/http_iso_org_iso3166-1_alpha2.vocab",
        "config/.vocabularies/http_rs_gbif_org_vocabulary_gbif_preservation_method.vocab",
        "config/.vocabularies/http_rs_gbif_org_vocabulary_dwc_basis_of_record.vocab",
        "config/.vocabularies/http_rs_gbif_org_vocabulary_gbif_dataset_type.vocab", "resources/res1/resource.xml",
        "resources/res1/eml.xml", "resources/res1/sources/occurrence.txt")) {
      copy("ipt3-datadir/" + f, ipt3.resolve(f));
    }
    Path ipt4 = root.resolve("ipt4");
    new Migrator(Map.of()).migrate(ipt3, ipt4, MigrationOptions.defaults());
    // not registered with the (unreachable) test registry, so it can be published here
    Path res1 = ipt4.resolve("resources/res1/resource.json");
    Files.writeString(res1, Files.readString(res1).replace("\"status\" : \"REGISTERED\"", "\"status\" : \"PUBLIC\"")
        .replaceAll("\"key\" : \"[^\"]+\",\n", "").replaceAll("\"organisation\" : \"[^\"]+\",\n", ""));
    copyResource(ipt4, "private1", "PRIVATE");
    copyResource(ipt4, "gone1", "DELETED");
    return ipt4;
  }

  /** Publishes res1 with IPT4, once per data dir. */
  public static void publishRes1(Path dataDir, org.gbif.ipt.service.manage.ResourceManager resources,
                                 org.gbif.ipt.service.manage.ResourcePublicationManager publications,
                                 org.gbif.ipt.service.admin.UserAccountManager users,
                                 org.gbif.ipt.service.admin.RegistrationManager registrations) throws Exception {
    org.gbif.ipt.model.Resource res1 = resources.get("res1");
    if (res1.getOrganisation() == null) {
      // what the setup wizard installs; IPT 3 published public resources with an organisation only
      org.gbif.ipt.model.Organisation none = registrations.get(org.gbif.ipt.config.Constants.DEFAULT_ORG_KEY);
      if (none == null) {
        none = new org.gbif.ipt.model.Organisation();
        none.setName("No organisation");
        none.setAlias("No organisation");
        none.setCanHost(true);
        none.setKey(org.gbif.ipt.config.Constants.DEFAULT_ORG_KEY.toString());
        none.setPassword("password");
        registrations.addAssociatedOrganisation(none);
        registrations.save();
      }
      res1.setOrganisation(none);
      resources.save(res1);
    }
    if (res1.getLastPublished() != null && dataDir.resolve("resources/res1/eml-" + res1.getEmlVersion().toPlainString()
        + ".xml").toFile().exists()) {
      return;
    }
    org.gbif.ipt.feedback.CollectingFeedback feedback =
        new org.gbif.ipt.feedback.CollectingFeedback(null, users.get(ADMIN));
    publications.publish(res1, res1.getNextVersion(), feedback);
    long deadline = System.currentTimeMillis() + 60_000;
    while (publications.isLocked("res1", feedback) && System.currentTimeMillis() < deadline) {
      Thread.sleep(100);
    }
    if (!feedback.getActionErrors().isEmpty()) {
      throw new IllegalStateException("Publishing res1 failed: " + feedback.getActionErrors());
    }
  }

  private static void copyResource(Path ipt4, String shortname, String status) throws IOException {
    Path dir = ipt4.resolve("resources/" + shortname);
    FileUtils.copyDirectory(ipt4.resolve("resources/res1").toFile(), dir.toFile());
    Path json = dir.resolve("resource.json");
    Files.writeString(json, Files.readString(json).replace("\"status\" : \"PUBLIC\"", "\"status\" : \"" + status + "\""));
  }

  private static void copy(String from, Path target) throws IOException {
    Files.createDirectories(target.getParent());
    try (InputStream in = PortalFixture.class.getClassLoader().getResourceAsStream(from)) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
