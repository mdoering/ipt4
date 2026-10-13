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

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Follows every link and GET form within the IPT from the home page, logged out and as admin: no errors, no template
 * code in the HTML. Covers the portal, the manage pages with the metadata editors, and the admin area.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrawlerTest {

  private static final String BASE = "http://localhost:8080";
  private static final String ADMIN = "crawler.admin@example.org";
  /** Logout (a POST), and the admin API and log files (not pages). */
  private static final List<String> NOT_YET = List.of("/logout", "/admin-api", "/admin/logfile");
  private static final List<String> TEMPLATE_CODE = List.of("${", "<@s", "[@s", "<#", "[#", "FreeMarker template error");
  private static final int MAX_PAGES = 400;
  /** IPT 3's data package previews link to the downloads before the first publication too (404 until then). */
  /** The registry's Occurrence core has a property with dc:relation='None', which the extension page links. */
  private static final List<String> REGISTRY_DATA_LINKS = List.of("/admin/None");
  private static final Pattern UNPUBLISHED_DOWNLOAD = Pattern.compile("/(archive|metadata)\\.do\\?r=(cam1|col1)(&v=.*)?");

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
    dataPackage("cam1", "camtrap-dp", "camtrap-datapackage.json", "datapackage.json");
    dataPackage("col1", "coldp", "coldp-metadata.yaml", "metadata.yaml");
  }

  private void dataPackage(String shortname, String type, String fixture, String fileName) throws Exception {
    if (resources.get(shortname) == null) {
      try (InputStream in = getClass().getResourceAsStream("/create/" + fixture)) {
        mvc.perform(multipart("/manage/create.do").file(new MockMultipartFile("file", fileName, "text/plain",
                in.readAllBytes())).param("shortname", shortname).param("resourceType", type)
            .with(user(ADMIN)).with(csrf()));
      }
    }
    assertThat(resources.get(shortname)).as(shortname).isNotNull();
  }

  @Test
  void loggedOut() throws Exception {
    assertThat(crawl(false)).hasSizeGreaterThan(15);
  }

  @Test
  void asAdmin() throws Exception {
    Set<String> seen = crawl(true);
    assertThat(seen).hasSizeGreaterThan(15);
    // the metadata editors of an EML resource and of a Camtrap DP resource
    assertThat(seen).anyMatch(url -> url.startsWith("/manage/metadata-additional"))
        .anyMatch(url -> url.startsWith("/admin/config")).anyMatch(url -> url.startsWith("/admin/users"))
        .anyMatch(url -> url.startsWith("/admin/user.do?id=")).anyMatch(url -> url.startsWith("/admin/extension.do?id="))
        .anyMatch(url -> url.startsWith("/admin/vocabulary")).anyMatch(url -> url.startsWith("/admin/dataPackage.do?id="))
        .anyMatch(url -> url.startsWith("/admin/logs")).anyMatch(url -> url.startsWith("/admin/bulk-publication"))
        .anyMatch(url -> url.startsWith("/manage/camtrap-metadata-other"))
        .anyMatch(url -> url.startsWith("/manage/resource") && url.contains("r=col1"));
  }

  /** The resource tables load their rows with the HTTP method the page declares (browsers do, the crawler doesn't). */
  @Test
  void resourceTablesLoadWithTheDeclaredMethod() throws Exception {
    Pattern ajax = Pattern.compile("url: *'([^']+)'.*?type: *'(\\w+)'", Pattern.DOTALL);
    for (String page : List.of("/", "/manage/")) {
      String html = mvc.perform(get(page).with(user(ADMIN))).andReturn().getResponse().getContentAsString();
      Matcher m = ajax.matcher(html);
      assertThat(m.find()).as(page).isTrue();
      String url = URI.create(m.group(1)).getPath() + "?draw=1&start=0&length=10";
      MockHttpServletResponse rows = mvc.perform(request(HttpMethod.valueOf(m.group(2)), url).with(user(ADMIN))
          .with(csrf())).andReturn().getResponse();
      assertThat(rows.getStatus()).as(m.group(2) + " " + url).isEqualTo(200);
      assertThat(rows.getContentAsString()).contains("aaData");
    }
  }

  private Set<String> crawl(boolean admin) throws Exception {
    MockHttpSession session = new MockHttpSession();
    Set<String> seen = new LinkedHashSet<>();
    // the home page loads its resource table with JavaScript from api/resources
    Deque<String> queue = new ArrayDeque<>(List.of("/", "/api/resources?start=0&length=100"));
    if (admin) {
      // the manage home loads its table with JavaScript too
      queue.addAll(List.of("/manage/", "/manager-api/resources?start=0&length=100", "/admin/"));
      // the user list and the organisation list open their forms with JavaScript
      queue.addAll(List.of("/admin/user.do", "/admin/user.do?id=" + ADMIN, "/admin/organisation.do"));
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
      if (response.getStatus() >= 400 && !(response.getStatus() == 404
          && (UNPUBLISHED_DOWNLOAD.matcher(url).matches() || REGISTRY_DATA_LINKS.contains(url)))) {
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
      snapshot(admin, url, response.getContentAsString());
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

  /** With -Dipt.snapshots=DIR, the page as normalised HTML in DIR (see {@link PageSnapshot}). */
  private static void snapshot(boolean admin, String url, String html) throws Exception {
    String dir = System.getProperty("ipt.snapshots");
    if (dir != null && !dir.isBlank()) {
      Path file = Path.of(dir, admin ? "admin" : "logged-out", PageSnapshot.fileName(url));
      Files.createDirectories(file.getParent());
      Files.writeString(file, PageSnapshot.normalise(html));
    }
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
