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

import org.gbif.dwc.Archive;
import org.gbif.dwc.ArchiveFile;
import org.gbif.dwc.DwcFiles;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.ipt.feedback.CollectingFeedback;
import org.gbif.ipt.legacy.MigrationOptions;
import org.gbif.ipt.legacy.MigrationReport;
import org.gbif.ipt.legacy.Migrator;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;
import org.gbif.utils.file.ClosableIterator;
import org.gbif.utils.file.CompressionUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An IPT 3 data dir is migrated, IPT4 starts on the result and publishes a Darwin Core Archive equal to
 * what IPT 3 produced for the same resource (the expectations of IPT 3's GenerateDwcaTest).
 */
@SpringBootTest
class MigrateAndPublishTest {

  static Path ipt4;
  static MigrationReport report;

  @DynamicPropertySource
  static void migrate(DynamicPropertyRegistry registry) throws Exception {
    Path root = Files.createTempDirectory("ipt-migrate-publish");
    Path ipt3 = root.resolve("ipt3");
    for (String f : List.of("config/ipt.properties", "config/users.xml", "config/.gbifreg",
        "config/.extensions/dwc_occurrence.xml", "config/.vocabularies/http_iso_org_639-2.vocab",
        "config/.vocabularies/http_iso_org_iso3166-1_alpha2.vocab",
        "config/.vocabularies/http_rs_gbif_org_vocabulary_gbif_preservation_method.vocab",
        "config/.vocabularies/http_rs_gbif_org_vocabulary_dwc_basis_of_record.vocab", "resources/res1/resource.xml", "resources/res1/eml.xml",
        "resources/res1/sources/occurrence.txt")) {
      Path target = ipt3.resolve(f);
      Files.createDirectories(target.getParent());
      try (InputStream in = MigrateAndPublishTest.class.getClassLoader().getResourceAsStream("ipt3-datadir/" + f)) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    ipt4 = root.resolve("ipt4");
    report = new Migrator(Map.of()).migrate(ipt3, ipt4, MigrationOptions.defaults());
    registry.add("ipt.data-dir", ipt4::toString);
  }

  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;

  @Test
  void publishesTheMigratedResourceLikeIpt3() throws Exception {
    assertThat(report.errors()).isEmpty();
    User kyle = users.get("kbraak@gbif.org");
    assertThat(kyle).isNotNull();

    Resource res1 = resources.get("res1");
    assertThat(res1).isNotNull();
    assertThat(res1.getCreator()).isSameAs(kyle);
    // not registered with this test's (unreachable) registry
    res1.setStatus(PublicationStatus.PUBLIC);
    res1.setKey(null);
    res1.setOrganisation(null);

    BigDecimal version = res1.getNextVersion();
    CollectingFeedback feedback = new CollectingFeedback(null, kyle);
    publications.publish(res1, version, feedback);
    long deadline = System.currentTimeMillis() + 60_000;
    while (publications.isLocked("res1", feedback) && System.currentTimeMillis() < deadline) {
      Thread.sleep(200);
    }
    assertThat(publications.isLocked("res1", feedback)).isFalse();
    assertThat(feedback.getActionErrors()).isEmpty();

    File dwca = ipt4.resolve("resources/res1/dwca-" + version.toPlainString() + ".zip").toFile();
    assertThat(dwca).exists();
    Path dir = Files.createTempDirectory("dwca");
    CompressionUtil.decompressFile(dir.toFile(), dwca, true);
    Archive archive = DwcFiles.fromLocation(dir);
    ArchiveFile core = archive.getCore();
    assertThat(core.getRowType()).isEqualTo(DwcTerm.Occurrence);
    assertThat(core.getId().getIndex()).isZero();
    assertThat(core.getFieldsSorted()).extracting(f -> f.getTerm().simpleName())
        .containsExactly("basisOfRecord", "occurrenceID", "scientificName", "kingdom");
    try (ClosableIterator<Record> it = core.iterator()) {
      Record r = it.next();
      assertThat(List.of(r.column(0), r.column(1), r.column(2), r.column(3), r.column(4)))
          .containsExactly("1", "occurrence", "1", "puma concolor", "occurrence");
      r = it.next();
      assertThat(List.of(r.column(0), r.column(1), r.column(2), r.column(3), r.column(4)))
          .containsExactly("2", "occurrence", "2", "pumm:concolor", "occurrence");
      assertThat(it.hasNext()).isFalse();
    }
    assertThat(ipt4.resolve("resources/res1/eml-" + version.toPlainString() + ".xml")).exists();
    assertThat(resources.get("res1").getRecordsPublished()).isEqualTo(2);
  }
}
