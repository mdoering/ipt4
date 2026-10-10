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
package org.gbif.ipt.web.portal;

import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.nio.file.Path;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;

/** IPT 3's ResourceFileAction, AppFileAction and DCATAction: same content types, file names and conditional GET. */
@SpringBootTest
@AutoConfigureMockMvc
class DownloadsTest {

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
  void publish() throws Exception {
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
  }

  private String version() {
    return resources.get("res1").getEmlVersion().toPlainString();
  }

  private MockHttpServletResponse fetch(String url) throws Exception {
    return mvc.perform(get(url)).andReturn().getResponse();
  }

  @Test
  void emlOfTheLatestVersion() throws Exception {
    MockHttpServletResponse r = fetch("/eml.do?r=res1");
    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(r.getContentType()).isEqualTo("text/xml;charset=UTF-8");
    assertThat(r.getHeader("Content-Disposition")).isEqualTo("filename=\"eml-res1-v" + version() + ".xml\"");
    assertThat(r.getContentAsString()).contains("eml:eml");
    assertThat(r.getHeader("Last-Modified")).isNotBlank();
  }

  @Test
  void archiveOfAVersion() throws Exception {
    MockHttpServletResponse r = fetch("/archive.do?r=res1&v=" + version());
    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(r.getContentType()).isEqualTo("application/zip");
    assertThat(r.getHeader("Content-Disposition")).isEqualTo("filename=\"dwca-res1-v" + version() + ".zip\"");
    assertThat(r.getContentAsByteArray()).startsWith((byte) 'P', (byte) 'K');
    // IPT 3 left caching of downloads to Last-Modified; no Spring Security no-store
    assertThat(r.getHeader("Cache-Control")).isNull();
    assertThat(r.getHeader("Pragma")).isNull();
    assertThat(fetch("/archive?r=res1&v=99.0").getStatus()).isEqualTo(404);
  }

  @Test
  void conditionalGetAndHead() throws Exception {
    String lastModified = fetch("/archive.do?r=res1").getHeader("Last-Modified");
    MockHttpServletResponse notModified =
        mvc.perform(get("/archive.do?r=res1").header("If-Modified-Since", lastModified)).andReturn().getResponse();
    assertThat(notModified.getStatus()).isEqualTo(304);
    assertThat(notModified.getContentAsByteArray()).isEmpty();
    MockHttpServletResponse headResponse = mvc.perform(head("/archive.do?r=res1")).andReturn().getResponse();
    assertThat(headResponse.getStatus()).isEqualTo(200);
    assertThat(headResponse.getContentType()).isEqualTo("application/zip");
  }

  @Test
  void malformedIfModifiedSinceServesTheFile() throws Exception {
    assertThat(mvc.perform(get("/eml.do?r=res1").header("If-Modified-Since", "yesterday-ish")).andReturn()
        .getResponse().getStatus()).isEqualTo(200);
  }

  @Test
  void rtfAndPublicationLog() throws Exception {
    assertThat(fetch("/rtf.do?r=res1&v=" + version()).getContentType()).isEqualTo("application/rtf");
    MockHttpServletResponse log = fetch("/publicationlog.do?r=res1");
    assertThat(log.getStatus()).isEqualTo(200);
    assertThat(log.getContentType()).startsWith("text/log");
  }

  @Test
  void sourceLogCannotEscapeTheResource() throws Exception {
    assertThat(fetch("/sourcelog.do?r=res1&s=../../config/users.json").getStatus()).isEqualTo(404);
    assertThat(fetch("/sourcelog.do?r=res1").getStatus()).isEqualTo(404);
    assertThat(fetch("/sourcelog.do?s=occurrence").getStatus()).isEqualTo(404);
  }

  @Test
  void missingLogosAreNotFound() throws Exception {
    assertThat(fetch("/logo.do?r=res1").getStatus()).isEqualTo(404);
    assertThat(fetch("/appLogo.do").getStatus()).isEqualTo(404);
  }

  @Test
  void privateResourcesStayPrivate() throws Exception {
    assertThat(fetch("/eml.do?r=private1").getStatus()).isEqualTo(401);
    assertThat(fetch("/archive.do?r=gone1").getStatus()).isEqualTo(410);
  }

  @Test
  void dcatFeed() throws Exception {
    MockHttpServletResponse r = fetch("/dcat.do");
    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(r.getContentType()).isEqualTo("text/turtle;charset=UTF-8");
    assertThat(r.getContentAsString()).contains("@prefix");
  }
}
