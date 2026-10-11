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
import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.app.StubRegistry;
import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** The overview's registry actions (IPT 3's OverviewAction) against a stub registry; one resource, in order. */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceRegistrationTest {

  private static final String ADMIN = "registry.admin@example.org";
  private static final String ORG = "fbca90e3-8aed-48b1-84e3-369afbd000ce";
  private static final String IPT_KEY = "7e5bd5f4-3b7a-4c5e-9d3e-0a1b2c3d4e5f";
  private static final String RESOURCE_KEY = "3f1c8a2e-6b4d-4f0a-9c7e-1d2b3a4c5e6f";
  private static final String NETWORK = "99748aa6-2ae5-4e0d-9a1b-5e2a3c4d5e6f";

  static Path dataDir;
  static StubRegistry stub;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
    stub = StubRegistry.start(registry);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired RegistrationManager registrations;

  /** A registered IPT hosted by an organisation that also publishes res1, which is public and published. */
  @BeforeEach
  void registeredIptAndPublicResource() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    if (registrations.getIpt() == null) {
      Organisation org = new Organisation();
      org.setKey(ORG);
      org.setName("GBIF Secretariat");
      org.setPassword("org-secret-1");
      org.setCanHost(true);
      registrations.addAssociatedOrganisation(org);
      registrations.addHostingOrganisation(org);
      Ipt ipt = new Ipt();
      ipt.setKey(IPT_KEY);
      ipt.setName("Test IPT");
      ipt.setOrganisationKey(ORG);
      ipt.setWsPassword("ws-secret-1");
      registrations.addIptInstance(ipt);
      registrations.save();

      Resource res1 = resources.get("res1");
      res1.setOrganisation(registrations.get(ORG));
      res1.setStatus(PublicationStatus.PUBLIC);
      // registration needs a licence GBIF supports in the published version
      res1.getEml().setIntellectualRights("This work is licensed under a <a "
          + "href=\"http://creativecommons.org/licenses/by/4.0/legalcode\">Creative Commons Attribution (CC-BY) 4.0 "
          + "License</a>.");
      resources.saveEml(res1);
      resources.save(res1);
      // res1's metadata is incomplete for the overview's publish button (IPT 3's fixture): publish directly
      publications.publish(res1, res1.getNextVersion(), new org.gbif.ipt.feedback.CollectingFeedback(null, users.get(ADMIN)));
      awaitPublication();
    }
  }

  private MockHttpServletResponse overview(String action, String... params) throws Exception {
    var request = post("/manage/" + action + ".do").param("r", "res1").with(user(ADMIN)).with(csrf());
    for (int i = 0; i < params.length; i += 2) {
      request.param(params[i], params[i + 1]);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  /** As the publish button does, without its metadata check (res1's metadata is incomplete). */
  private void publish() throws Exception {
    Resource res1 = resources.get("res1");
    publications.publish(res1, res1.getNextVersion(), new org.gbif.ipt.feedback.CollectingFeedback(null, users.get(ADMIN)));
    awaitPublication();
  }

  private void awaitPublication() throws Exception {
    long deadline = System.currentTimeMillis() + 60_000;
    while (publications.isLocked("res1") && System.currentTimeMillis() < deadline) {
      Thread.sleep(100);
    }
    assertThat(publications.isLocked("res1")).isFalse();
  }

  private List<StubRegistry.Request> requests(String method, String path) {
    return stub.requests().stream().filter(r -> r.method().equals(method) && r.path().startsWith(path)).toList();
  }

  @Test
  @Order(1)
  void registerTheResource() throws Exception {
    stub.answer("POST", "/registry/ipt/resource", 201, "application/xml",
        "<resource><key>" + RESOURCE_KEY + "</key></resource>", 500);
    // a double click: two registrations at once must create one registry entry
    java.util.concurrent.ExecutorService clicks = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var first = clicks.submit(() -> overview("resource-registerResource"));
      var second = clicks.submit(() -> overview("resource-registerResource"));
      first.get(30, java.util.concurrent.TimeUnit.SECONDS);
      second.get(30, java.util.concurrent.TimeUnit.SECONDS);
    } finally {
      clicks.shutdownNow();
    }
    assertThat(stub.requests().stream().filter(r -> r.method().equals("POST")
        && r.path().equals("/registry/ipt/resource"))).hasSize(1);
    Resource res1 = resources.get("res1");
    assertThat(res1.getStatus()).isEqualTo(PublicationStatus.REGISTERED);
    assertThat(res1.getKey()).hasToString(RESOURCE_KEY);
    StubRegistry.Request registration = requests("POST", "/registry/ipt/resource").get(0);
    assertThat(registration.user()).isEqualTo(ORG);
    assertThat(registration.body()).contains("organisationKey=" + ORG).contains("iptKey=" + IPT_KEY);
  }

  @Test
  @Order(2)
  void publishingARegisteredResourceUpdatesTheRegistry() throws Exception {
    stub.answer("POST", "/registry/ipt/resource/" + RESOURCE_KEY, 200, "application/xml", "<resource/>");
    int before = requests("POST", "/registry/ipt/resource/" + RESOURCE_KEY).size();
    publish();
    assertThat(requests("POST", "/registry/ipt/resource/" + RESOURCE_KEY)).hasSize(before + 1)
        .allSatisfy(r -> assertThat(r.user()).isEqualTo(ORG));
  }

  /** IPT 3 fails a publication whose registry update fails: the previous version stays the published one. */
  @Test
  @Order(3)
  void registryDownDuringPublication() throws Exception {
    stub.answer("POST", "/registry/ipt/resource/" + RESOURCE_KEY, 500, "text/plain", "down");
    BigDecimal before = resources.get("res1").getEmlVersion();
    // as if the last published archive had other data than this one
    String checksum = "previous-archive-checksum";
    java.util.Map<String, Integer> counts = java.util.Map.of("http://rs.tdwg.org/dwc/terms/Occurrence", 1);
    resources.get("res1").setLastPublishedArchiveChecksum(checksum);
    resources.get("res1").setRecordsByExtension(new java.util.HashMap<>(counts));
    publish();
    assertThat(resources.get("res1").getEmlVersion()).isEqualTo(before);
    // the failed archive's checksum would make "skip if not changed" skip its data later
    assertThat(resources.get("res1").getLastPublishedArchiveChecksum()).isEqualTo(checksum);
    assertThat(resources.get("res1").getRecordsByExtension()).isEqualTo(counts);
    assertThat(resources.get("res1").getStatus()).isEqualTo(PublicationStatus.REGISTERED);
    assertThat(publications.isLocked("res1")).isFalse();
    stub.answer("POST", "/registry/ipt/resource/" + RESOURCE_KEY, 200, "application/xml", "<resource/>");
  }

  @Test
  @Order(4)
  void addAndRemoveANetwork() throws Exception {
    stub.answer("GET", "/registry/network.json", 200, "application/json",
        "[{\"key\":\"" + NETWORK + "\",\"name\":\"Test network\"}]");
    stub.answer("POST", "/registry/resource/" + RESOURCE_KEY + "/network/" + NETWORK, 200, "text/plain", "");
    stub.answer("DELETE", "/registry/resource/" + RESOURCE_KEY + "/network/" + NETWORK, 200, "text/plain", "");
    overview("resource-addNetwork", "id", NETWORK);
    assertThat(requests("POST", "/registry/resource/" + RESOURCE_KEY + "/network/" + NETWORK)).hasSize(1);
    overview("resource-deleteNetwork", "id", NETWORK);
    assertThat(requests("DELETE", "/registry/resource/" + RESOURCE_KEY + "/network/" + NETWORK)).hasSize(1);
  }

  @Test
  @Order(5)
  void deletingARegisteredResourceDeregistersIt() throws Exception {
    stub.answer("DELETE", "/registry/ipt/resource/" + RESOURCE_KEY, 200, "text/plain", "deleted");
    overview("resource-delete", "deleteFlag", "Delete");
    assertThat(requests("DELETE", "/registry/ipt/resource/" + RESOURCE_KEY)).hasSize(1)
        .allSatisfy(r -> assertThat(r.user()).isEqualTo(ORG));
    assertThat(resources.get("res1") == null || resources.get("res1").getStatus() == PublicationStatus.DELETED).isTrue();
  }
}
