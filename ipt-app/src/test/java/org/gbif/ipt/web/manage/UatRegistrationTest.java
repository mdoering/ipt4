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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Registers an IPT and a resource in GBIF UAT, publishes it with a DOI from DataCite test, and cleans up (resource
 * deleted and deregistered, DOI deactivated). It creates real records: run only on purpose, with
 * {@code mvn test -pl ipt-app -DexcludedGroups= -Dgroups=uat -Dtest=UatRegistrationTest} and the environment
 * variables IPT_UAT_ORG_KEY, IPT_UAT_ORG_PASSWORD, IPT_DATACITE_TEST_USER, IPT_DATACITE_TEST_PASSWORD,
 * IPT_DATACITE_TEST_PREFIX and IPT_UAT_PUBLIC_URL (a base URL the registry accepts for the test IPT).
 */
@Tag("uat")
@SpringBootTest
@AutoConfigureMockMvc
class UatRegistrationTest {

  private static final String ADMIN = "uat.admin@example.org";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
    registry.add("ipt.defaults[dev.registrydev.url]", () -> "https://gbrds.gbif-uat.org");
    registry.add("ipt.defaults[dev.datacitedev.url]", () -> "https://api.test.datacite.org");
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired RegistrationManager registrations;
  @Autowired ConfigManager configManager;

  private static String env(String name) {
    String value = System.getenv(name);
    assumeTrue(value != null && !value.isBlank(), name + " is not set");
    return value;
  }

  private void send(String url, String... params) throws Exception {
    var request = post(url).with(user(ADMIN)).with(csrf());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    mvc.perform(request);
  }

  private void awaitPublication(String shortname) throws Exception {
    long deadline = System.currentTimeMillis() + 300_000;
    while (publications.isLocked(shortname) && System.currentTimeMillis() < deadline) {
      Thread.sleep(500);
    }
  }

  @Test
  void registerPublishWithADoiAndCleanUp() throws Exception {
    String orgKey = env("IPT_UAT_ORG_KEY");
    String orgPassword = env("IPT_UAT_ORG_PASSWORD");
    String dataciteUser = env("IPT_DATACITE_TEST_USER");
    String datacitePassword = env("IPT_DATACITE_TEST_PASSWORD");
    String prefix = env("IPT_DATACITE_TEST_PREFIX");
    String publicUrl = env("IPT_UAT_PUBLIC_URL");
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    configManager.setBaseUrl(java.net.URI.create(publicUrl).toURL());
    configManager.setArchivalMode(true);
    configManager.saveConfig();

    // the IPT, hosted by the organisation, which holds the DataCite test account
    send("/admin/organisation.do", "organisation.key", orgKey, "organisation.password", orgPassword,
        "organisation.canHost", "true", "save", "Save");
    Organisation org = registrations.get(orgKey);
    assertThat(org).as("organisation associated (password accepted by UAT)").isNotNull();
    org.setDoiRegistrationAgency(DOIRegistrationAgency.DATACITE);
    org.setAgencyAccountUsername(dataciteUser);
    org.setAgencyAccountPassword(datacitePassword);
    org.setDoiPrefix(prefix);
    org.setAgencyAccountPrimary(true);
    registrations.save();
    send("/admin/registration.do", "organisation.key", orgKey, "organisation.password", orgPassword,
        "organisation.name", org.getName(), "ipt.organisationKey", orgKey, "ipt.name", "IPT4 UAT test",
        "ipt.description", "IPT4 test installation, removed after the test", "ipt.primaryContactName", "IPT4 test",
        "ipt.primaryContactEmail", "helpdesk@gbif.org", "ipt.primaryContactType", "technical",
        "ipt.wsPassword", "ipt4-uat-" + System.currentTimeMillis(), "save", "Save");
    assertThat(registrations.getIpt()).as("IPT registered in UAT").isNotNull();

    // a resource with complete metadata, a DOI, published and registered
    try (InputStream in = getClass().getResourceAsStream("/create/eml.xml")) {
      mvc.perform(multipart("/manage/create.do").file(new MockMultipartFile("file", "eml.xml", "text/xml",
          in.readAllBytes())).param("shortname", "uat1").param("resourceType", "occurrence")
          .with(user(ADMIN)).with(csrf()));
    }
    Resource uat1 = resources.get("uat1");
    uat1.setOrganisation(org);
    uat1.setStatus(PublicationStatus.PUBLIC);
    resources.save(uat1);
    send("/manage/resource-reserveDoi.do", "r", "uat1", "reserveDoi", "true");
    assertThat(resources.get("uat1").getDoi()).as("DOI reserved").isNotNull();
    send("/manage/publish.do", "r", "uat1", "publish", "Publish", "summary", "IPT4 UAT test");
    awaitPublication("uat1");
    assertThat(resources.get("uat1").getIdentifierStatus()).isEqualTo(IdentifierStatus.PUBLIC);
    send("/manage/resource-registerResource.do", "r", "uat1");
    assertThat(resources.get("uat1").getKey()).as("resource registered in UAT").isNotNull();

    // clean up: deregister, deactivate the DOI
    send("/manage/resource-delete.do", "r", "uat1", "deleteFlag", "Delete");
    assertThat(resources.get("uat1") == null || resources.get("uat1").getStatus() == PublicationStatus.DELETED)
        .isTrue();
  }
}
