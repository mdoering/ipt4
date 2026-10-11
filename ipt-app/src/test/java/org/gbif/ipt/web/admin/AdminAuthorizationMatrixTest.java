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
package org.gbif.ipt.web.admin;

import org.gbif.ipt.app.ManageFixture;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Every admin and admin API handler, enumerated from the request mappings, called by an anonymous visitor (login), a
 * plain user and a manager (401), and an admin (not stopped; POSTs need the CSRF token). IPT 3 left bulkReport without
 * any check.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthorizationMatrixTest {

  private static final String ADMIN = "matrix.admin@example.org";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

  record Endpoint(String path, RequestMethod method) {
  }

  @BeforeEach
  void users() throws Exception {
    ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    ManageFixture.user(users, "plain@example.org", User.Role.User);
    ManageFixture.user(users, ADMIN, User.Role.Admin);
  }

  private List<Endpoint> endpoints() {
    List<Endpoint> endpoints = new ArrayList<>();
    for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
      for (String pattern : info.getPatternValues()) {
        if (!pattern.startsWith("/admin") || pattern.startsWith("/admin/setup")) {
          continue;
        }
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        if (methods.isEmpty() || methods.contains(RequestMethod.GET)) {
          endpoints.add(new Endpoint(pattern, RequestMethod.GET));
        }
        if (methods.isEmpty() || methods.contains(RequestMethod.POST)) {
          endpoints.add(new Endpoint(pattern, RequestMethod.POST));
        }
      }
    }
    assertThat(endpoints).isNotEmpty();
    return endpoints;
  }

  private MockHttpServletRequestBuilder request(Endpoint e, boolean withCsrf) {
    MockHttpServletRequestBuilder b = e.method() == RequestMethod.GET ? get(e.path()) : post(e.path());
    if (withCsrf && e.method() == RequestMethod.POST) {
      b.with(csrf());
    }
    return b.param("r", "res1").param("id", "x");
  }

  @Test
  void anonymousVisitorsLogIn() throws Exception {
    for (Endpoint e : endpoints()) {
      assertThat(mvc.perform(request(e, true)).andReturn().getResponse().getRedirectedUrl()).as(e.toString())
          .endsWith("/login");
    }
  }

  @Test
  void usersAndManagersAreRefused() throws Exception {
    for (String who : List.of("plain@example.org", ManageFixture.OWNER)) {
      for (Endpoint e : endpoints()) {
        assertThat(mvc.perform(request(e, true).with(user(who))).andReturn().getResponse().getStatus())
            .as(who + " " + e).isEqualTo(401);
      }
    }
  }

  @Test
  void adminsAreNotStopped() throws Exception {
    for (Endpoint e : endpoints()) {
      int status;
      try {
        status = mvc.perform(request(e, true).with(user(ADMIN))).andReturn().getResponse().getStatus();
      } catch (jakarta.servlet.ServletException failedInThePage) {
        // past the access checks; the junk id breaks some IPT 3 pages
        status = 500;
      }
      assertThat(status).as(e.toString()).isNotIn(401, 403);
    }
  }

  @Test
  void postsNeedTheCsrfToken() throws Exception {
    for (Endpoint e : endpoints()) {
      if (e.method() == RequestMethod.POST) {
        assertThat(mvc.perform(request(e, false).with(user(ADMIN))).andReturn().getResponse().getStatus())
            .as(e.toString()).isEqualTo(403);
      }
    }
  }

  @Test
  void adminHomeLinksTheAdminPages() throws Exception {
    String html = mvc.perform(get("/admin/").with(user(ADMIN))).andReturn().getResponse().getContentAsString();
    assertThat(html).contains("/admin/config.do", "/admin/users.do", "/admin/extensions.do", "/admin/logs.do")
        .doesNotContain("FreeMarker template error");
  }
}
