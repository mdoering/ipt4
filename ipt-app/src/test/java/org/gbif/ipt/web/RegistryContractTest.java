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
package org.gbif.ipt.web;

import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.voc.PublicationStatus;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The URLs the GBIF registry stores for IPT datasets and installations (endpoints, homepages, RSS and DCAT feeds)
 * keep answering as on IPT 3, with and without {@code .do}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistryContractTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = PortalFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired UserAccountManager users;
  @Autowired RegistrationManager registrations;

  @BeforeEach
  void registeredResource() throws Exception {
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    Resource res1 = resources.get("res1");
    res1.setKey(UUID.fromString("57dfbfb4-6213-4df5-aa5b-4adc3d213ebf"));
    res1.setStatus(PublicationStatus.REGISTERED);
  }

  private void answers(String url, String contentType) throws Exception {
    MockHttpServletResponse r = mvc.perform(get(url)).andReturn().getResponse();
    assertThat(r.getStatus()).as(url).isEqualTo(200);
    assertThat(r.getContentType()).as(url).startsWith(contentType);
    assertThat(r.getContentLength() > 0 || r.getContentAsByteArray().length > 0).as(url).isTrue();
  }

  @Test
  void datasetEndpoints() throws Exception {
    String v = resources.get("res1").getEmlVersion().toPlainString();
    answers("/archive.do?r=res1", "application/zip");
    answers("/archive.do?r=res1&v=" + v, "application/zip");
    answers("/eml.do?r=res1", "text/xml");
    answers("/eml.do?r=res1&v=" + v, "text/xml");
  }

  @Test
  void datasetHomepage() throws Exception {
    answers("/resource?r=res1", "text/html");
    answers("/resource.do?r=res1", "text/html");
    answers("/resource?r=res1&v=" + resources.get("res1").getEmlVersion().toPlainString(), "text/html");
  }

  @Test
  void installationFeeds() throws Exception {
    answers("/rss.do", "text/xml");
    answers("/rss", "text/xml");
    answers("/dcat.do", "text/turtle");
    answers("/inventory/dataset", "application/json");
    assertThat(mvc.perform(get("/inventory/dataset")).andReturn().getResponse().getContentAsString())
        .contains("57dfbfb4-6213-4df5-aa5b-4adc3d213ebf");
  }
}
