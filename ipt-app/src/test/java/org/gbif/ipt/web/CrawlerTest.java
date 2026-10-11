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
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Follows every link and GET form within the IPT from the home page, logged out and as admin: no errors, no template
 * code in the HTML. The admin area (M5) and the metadata editors (M4) are not followed yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrawlerTest {

  private static final String BASE = "http://localhost:8080";
  private static final String ADMIN = "crawler.admin@example.org";
  /** The admin area (M5), the metadata editors (M4) and logout (a POST). */
  private static final List<String> NOT_YET = List.of("/admin", "/logout", "/manage/metadata-",
      "/manage/camtrap-metadata-", "/manage/datapackage-metadata-", "/manage/uploadlogo");
  private static final List<String> TEMPLATE_CODE = List.of("${", "<@s", "[@s", "<#", "[#", "FreeMarker template error");
  private static final int MAX_PAGES = 400;

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = org.gbif.ipt.app.ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired UserAccountManager users;
  @Autowired RegistrationManager registrations;

  @BeforeEach
  void publishedResourceAndAdmin() throws Exception {
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    if (users.get(ADMIN) == null) {
      User admin = new User();
      admin.setEmail(ADMIN);
      admin.setFirstname("Crawler");
      admin.setLastname("Admin");
      admin.setRole(User.Role.Admin);
      admin.setPassword("Secret-pass-1");
      users.create(admin);
    }
  }

  @Test
  void loggedOut() throws Exception {
    assertThat(crawl(false)).hasSizeGreaterThan(15);
  }

  @Test
  void asAdmin() throws Exception {
    assertThat(crawl(true)).hasSizeGreaterThan(15);
  }

  private Set<String> crawl(boolean admin) throws Exception {
    MockHttpSession session = new MockHttpSession();
    Set<String> seen = new LinkedHashSet<>();
    // the home page loads its resource table with JavaScript from api/resources
    Deque<String> queue = new ArrayDeque<>(List.of("/", "/api/resources?start=0&length=100"));
    if (admin) {
      // the manage home loads its table with JavaScript too
      queue.addAll(List.of("/manage/", "/manager-api/resources?start=0&length=100"));
    }
    List<String> problems = new ArrayList<>();
    while (!queue.isEmpty() && seen.size() < MAX_PAGES) {
      String url = queue.poll();
      if (!seen.add(url)) {
        continue;
      }
      MockHttpServletRequestBuilder request = get(URI.create(url)).session(session);
      if (admin) {
        request.with(user(ADMIN));
      }
      MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
      if (response.getStatus() >= 300 && response.getStatus() < 400) {
        String location = local(response.getRedirectedUrl(), url);
        if (location != null) {
          queue.add(location);
        }
        continue;
      }
      if (response.getStatus() >= 400) {
        problems.add(response.getStatus() + " " + url);
        continue;
      }
      String type = response.getContentType();
      if (type != null && type.startsWith("application/json")
          && (url.startsWith("/api/resources") || url.startsWith("/manager-api/resources"))) {
        for (JsonNode row : JsonMapper.shared().readTree(response.getContentAsString()).get("aaData")) {
          for (JsonNode cell : row) {
            for (Element a : Jsoup.parse(cell.asString(), BASE + url).select("a[href]")) {
              enqueue(queue, seen, local(a.attr("href"), url));
            }
          }
        }
        continue;
      }
      if (type == null || !type.startsWith("text/html")) {
        continue;
      }
      Document doc = Jsoup.parse(response.getContentAsString(), BASE + url);
      doc.select("script").remove();
      String html = doc.html();
      for (String code : TEMPLATE_CODE) {
        if (html.contains(code)) {
          problems.add("template code " + code + " in " + url);
        }
      }
      // links IPT4 sends as POST forms (csrf.js iptPostLink)
      for (Element a : doc.select("a[href]:not([data-method=post]):not([data-post]):not([href*=/manage/cancel.do])")) {
        enqueue(queue, seen, local(a.attr("href"), url));
      }
      for (Element form : doc.select("form")) {
        if (!"post".equalsIgnoreCase(form.attr("method"))) {
          enqueue(queue, seen, local(form.attr("action"), url));
        }
      }
    }
    assertThat(problems).as("pages visited: " + seen).isEmpty();
    return seen;
  }

  private static void enqueue(Deque<String> queue, Set<String> seen, String url) {
    if (url == null || seen.contains(url) || NOT_YET.stream().anyMatch(url::startsWith)) {
      return;
    }
    // each page links to itself in every language; follow the language links of the home page only
    if (url.contains("request_locale") && !(url.startsWith("/?") || url.startsWith("/home"))) {
      return;
    }
    queue.add(url);
  }

  /** The path and query of a link within this IPT, or null for other sites, anchors and scripts. */
  private static String local(String href, String from) {
    if (href == null || href.isBlank() || href.startsWith("#") || href.startsWith("mailto:")
        || href.startsWith("javascript:")) {
      return null;
    }
    URI uri;
    try {
      uri = URI.create(BASE + from).resolve(href.trim().replace(" ", "%20"));
    } catch (IllegalArgumentException e) {
      return null;
    }
    if (!"localhost".equals(uri.getHost()) || (uri.getPort() != 8080 && uri.getPort() != -1)) {
      return null;
    }
    String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
    return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
  }
}
