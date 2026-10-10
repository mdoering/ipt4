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
import org.gbif.ipt.feedback.CollectingFeedback;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
class PortalPagesTest {

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

  /** res1 is published once, by IPT4, before the first test. */
  @BeforeEach
  void publishRes1() throws Exception {
    Resource res1 = resources.get("res1");
    if (res1.getLastPublished() != null && dataDir.resolve("resources/res1/eml-" + res1.getEmlVersion().toPlainString()
        + ".xml").toFile().exists()) {
      return;
    }
    CollectingFeedback feedback = new CollectingFeedback(null, users.get(PortalFixture.ADMIN));
    publications.publish(res1, res1.getNextVersion(), feedback);
    long deadline = System.currentTimeMillis() + 60_000;
    while (publications.isLocked("res1", feedback) && System.currentTimeMillis() < deadline) {
      Thread.sleep(100);
    }
    assertThat(feedback.getActionErrors()).isEmpty();
  }

  private MvcResult get200(String url) throws Exception {
    MvcResult result = mvc.perform(get(url)).andReturn();
    assertThat(result.getResponse().getStatus()).as(url).isEqualTo(200);
    return result;
  }

  private String html(String url) throws Exception {
    return get200(url).getResponse().getContentAsString();
  }

  @Test
  void homeListsThePortal() throws Exception {
    assertThat(html("/")).contains("<title>").contains("api/resources");
    assertThat(html("/home.do")).contains("<title>");
  }

  @Test
  void resourcePageShowsThePublishedVersion() throws Exception {
    String html = html("/resource?r=res1");
    assertThat(html).contains("archive.do?r=res1").contains("eml.do?r=res1");
    assertThat(html("/resource.do?r=res1")).contains("archive.do?r=res1");
  }

  @Test
  void unknownResourcesAreNotFound() throws Exception {
    MvcResult result = mvc.perform(get("/resource?r=nope")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(404);
    assertThat(mvc.perform(get("/resource?r=")).andReturn().getResponse().getStatus()).isEqualTo(404);
  }

  @Test
  void privateResourceIsHiddenFromAnonymous() throws Exception {
    // IPT 3 answered with its 401 page
    assertThat(mvc.perform(get("/resource?r=private1")).andReturn().getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void deletedResourceIsGone() throws Exception {
    assertThat(mvc.perform(get("/resource?r=gone1")).andReturn().getResponse().getStatus()).isEqualTo(410);
  }

  @Test
  void badVersionIs404() throws Exception {
    assertThat(mvc.perform(get("/resource?r=res1&v=abc")).andReturn().getResponse().getStatus()).isEqualTo(404);
  }

  @Test
  void previewNeedsALogin() throws Exception {
    MvcResult result = mvc.perform(get("/resource/preview?r=res1")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(302);
    assertThat(result.getResponse().getRedirectedUrl()).contains("/login");
  }

  @Test
  @WithMockUser(username = PortalFixture.ADMIN, roles = "ADMIN")
  void managersSeeThePreviewAndPrivateResources() throws Exception {
    assertThat(html("/resource/preview?r=res1")).contains("res1");
    assertThat(mvc.perform(get("/resource?r=private1")).andReturn().getResponse().getStatus()).isNotEqualTo(401);
  }

  @Test
  void aboutHealthFallbackAndRss() throws Exception {
    assertThat(html("/about")).contains("About this IPT installation");
    assertThat(html("/health.do")).contains("<title>");
    assertThat(html("/fallback")).contains("<title>");
    MvcResult rss = get200("/rss.do");
    assertThat(rss.getResponse().getContentType()).startsWith("text/xml");
    assertThat(rss.getResponse().getContentAsString()).contains("<rss").contains("resource?r=res1");
  }
}
