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
import org.gbif.ipt.app.StubDataCite;
import org.gbif.ipt.app.StubRegistry;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.DOIRegistrationAgency;
import org.gbif.ipt.model.voc.IdentifierStatus;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.admin.ConfigManager;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.io.InputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** The overview's DOI actions and publishing with a DOI (IPT 3's OverviewAction) against a stub DataCite. */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceDoiTest {

  private static final String ADMIN = "doi.admin@example.org";
  private static final String ORG = "fbca90e3-8aed-48b1-84e3-369afbd000ce";
  private static final String DATACITE_USER = "GBIF.TEST";
  private static final String DATACITE_PASSWORD = "datacite-secret-1";
  private static final String PREFIX = "10.80000";

  static Path dataDir;
  static StubDataCite datacite;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
    StubRegistry.start(registry);
    datacite = StubDataCite.start(registry, DATACITE_USER, DATACITE_PASSWORD);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired RegistrationManager registrations;
  @Autowired ConfigManager configManager;

  /** An organisation with the primary DataCite account (archival mode on), publishing complete resources. */
  @BeforeEach
  void doiAccount() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    if (registrations.get(ORG) == null) {
      configManager.setArchivalMode(true);
      configManager.saveConfig();
      Organisation org = new Organisation();
      org.setKey(ORG);
      org.setName("GBIF Secretariat");
      org.setPassword("org-secret-1");
      org.setCanHost(true);
      org.setDoiRegistrationAgency(DOIRegistrationAgency.DATACITE);
      org.setAgencyAccountUsername(DATACITE_USER);
      org.setAgencyAccountPassword(DATACITE_PASSWORD);
      org.setDoiPrefix(PREFIX);
      org.setAgencyAccountPrimary(true);
      registrations.addAssociatedOrganisation(org);
      registrations.save();
    }
    for (String shortname : new String[] {"doi1", "doi2", "doi3"}) {
      if (resources.get(shortname) == null) {
        try (InputStream in = getClass().getResourceAsStream("/create/eml.xml")) {
          mvc.perform(multipart("/manage/create.do").file(new MockMultipartFile("file", "eml.xml", "text/xml",
                  in.readAllBytes())).param("shortname", shortname).param("resourceType", "occurrence")
              .with(user(ADMIN)).with(csrf()));
        }
        Resource resource = resources.get(shortname);
        resource.setOrganisation(registrations.get(ORG));
        resources.save(resource);
      }
    }
  }

  private void overview(String shortname, String action, String... params) throws Exception {
    var request = post("/manage/" + action + ".do").param("r", shortname).with(user(ADMIN)).with(csrf());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    mvc.perform(request);
  }

  private void publish(String shortname) throws Exception {
    overview(shortname, "publish", "publish", "Publish", "summary", "with a DOI");
    long deadline = System.currentTimeMillis() + 60_000;
    while (publications.isLocked(shortname) && System.currentTimeMillis() < deadline) {
      Thread.sleep(100);
    }
    assertThat(publications.isLocked(shortname)).isFalse();
  }

  private StubDataCite.Doi stored(Resource resource) {
    return datacite.dois().get(resource.getDoi().getDoiName());
  }

  @Test
  @Order(1)
  void reserveADoi() throws Exception {
    overview("doi1", "resource-reserveDoi", "reserveDoi", "true");
    Resource doi1 = resources.get("doi1");
    assertThat(doi1.getDoi()).isNotNull();
    assertThat(doi1.getDoi().getPrefix()).isEqualTo(PREFIX);
    assertThat(doi1.getIdentifierStatus()).isEqualTo(IdentifierStatus.PUBLIC_PENDING_PUBLICATION);
    assertThat(stored(doi1).state()).isEqualTo("draft");
    assertThat(datacite.requests()).isNotEmpty().allSatisfy(r -> assertThat(r.user()).isEqualTo(DATACITE_USER));
  }

  @Test
  @Order(2)
  void publishingRegistersTheDoi() throws Exception {
    Resource doi1 = resources.get("doi1");
    doi1.setStatus(PublicationStatus.PUBLIC);
    resources.save(doi1);
    publish("doi1");
    doi1 = resources.get("doi1");
    assertThat(doi1.getIdentifierStatus()).isEqualTo(IdentifierStatus.PUBLIC);
    assertThat(stored(doi1).state()).isEqualTo("findable");
    assertThat(stored(doi1).url()).endsWith("/resource?r=doi1");
    assertThat(stored(doi1).xml()).containsIgnoringCase(doi1.getDoi().getDoiName()).contains("<creatorName>");
  }

  @Test
  @Order(3)
  void republishingUpdatesTheDoi() throws Exception {
    long updates = datacite.requests().stream().filter(r -> r.method().equals("PUT")).count();
    publish("doi1");
    assertThat(datacite.requests().stream().filter(r -> r.method().equals("PUT")).count()).isGreaterThan(updates);
    assertThat(stored(resources.get("doi1")).state()).isEqualTo("findable");
  }

  @Test
  @Order(4)
  void deleteAReservedDoi() throws Exception {
    overview("doi2", "resource-reserveDoi", "reserveDoi", "true");
    String doi = resources.get("doi2").getDoi().getDoiName();
    assertThat(datacite.dois()).containsKey(doi);
    overview("doi2", "resource-deleteDoi", "deleteDoi", "true");
    assertThat(datacite.dois()).doesNotContainKey(doi);
    assertThat(resources.get("doi2").getDoi()).isNull();
    assertThat(resources.get("doi2").getIdentifierStatus()).isEqualTo(IdentifierStatus.UNRESERVED);
  }

  /** DataCite refusing a reservation leaves the resource without a DOI. */
  @Test
  @Order(5)
  void dataCiteFailureLeavesTheResourceConsistent() throws Exception {
    datacite.failWith(500);
    try {
      overview("doi3", "resource-reserveDoi", "reserveDoi", "true");
    } finally {
      datacite.failWith(0);
    }
    Resource doi3 = resources.get("doi3");
    assertThat(doi3.getDoi()).isNull();
    assertThat(doi3.getIdentifierStatus()).isEqualTo(IdentifierStatus.UNRESERVED);
  }

  /** DataCite and organisation passwords never reach the log, also when DataCite refuses. */
  @Test
  @Order(6)
  @ExtendWith(OutputCaptureExtension.class)
  void secretsAreNotLogged(CapturedOutput output) throws Exception {
    overview("doi3", "resource-reserveDoi", "reserveDoi", "true");
    overview("doi3", "resource-deleteDoi", "deleteDoi", "true");
    datacite.failWith(401);
    try {
      overview("doi3", "resource-reserveDoi", "reserveDoi", "true");
    } finally {
      datacite.failWith(0);
    }
    assertThat(output.getAll()).contains("Reserving").doesNotContain(DATACITE_PASSWORD).doesNotContain("org-secret-1");
  }
}
