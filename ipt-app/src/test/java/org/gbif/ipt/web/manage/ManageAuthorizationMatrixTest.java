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
 * Every manage and manager API handler, enumerated from the request mappings, called by an anonymous visitor (login),
 * a plain user (401), a manager of another resource asking for res1 (401) and an admin (not stopped). IPT 3 left
 * cancel, peek, mappingPeek and report without any check.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManageAuthorizationMatrixTest {

  /** Handlers that work on no resource, so a manager of other resources may call them with any r. */
  private static final Set<String> WITHOUT_RESOURCE = Set.of("/manage/urlMetadata");

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
    ManageFixture.user(users, ManageFixture.OTHER, User.Role.Manager);
    ManageFixture.user(users, "plain@example.org", User.Role.User);
    ManageFixture.user(users, "matrix.admin@example.org", User.Role.Admin);
  }

  private List<Endpoint> endpoints() {
    List<Endpoint> endpoints = new ArrayList<>();
    for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
      for (String pattern : info.getPatternValues()) {
        if (!pattern.startsWith("/manage") && !pattern.startsWith("/manager-api")) {
          continue;
        }
        String path = pattern.replace("{method}", "makePrivate").replace("{section}", "basic");
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        endpoints.add(new Endpoint(path, methods.isEmpty() || methods.contains(RequestMethod.GET)
            ? RequestMethod.GET : RequestMethod.POST));
      }
    }
    assertThat(endpoints).hasSizeGreaterThan(30);
    return endpoints;
  }

  private MockHttpServletRequestBuilder request(Endpoint e) {
    MockHttpServletRequestBuilder b = e.method() == RequestMethod.GET ? get(e.path()) : post(e.path()).with(csrf());
    return b.param("r", "res1").param("id", "x").param("mid", "0");
  }

  @Test
  void anonymousVisitorsLogIn() throws Exception {
    for (Endpoint e : endpoints()) {
      assertThat(mvc.perform(request(e)).andReturn().getResponse().getRedirectedUrl()).as(e.toString())
          .endsWith("/login");
    }
  }

  @Test
  void plainUsersAreRefused() throws Exception {
    for (Endpoint e : endpoints()) {
      assertThat(mvc.perform(request(e).with(user("plain@example.org"))).andReturn().getResponse().getStatus())
          .as(e.toString()).isEqualTo(401);
    }
  }

  @Test
  void managersCannotTouchOthersResources() throws Exception {
    for (Endpoint e : endpoints()) {
      if (WITHOUT_RESOURCE.contains(e.path())) {
        continue;
      }
      assertThat(mvc.perform(request(e).with(user(ManageFixture.OTHER))).andReturn().getResponse().getStatus())
          .as(e.toString()).isEqualTo(401);
    }
  }

  @Test
  void adminsAreNotStopped() throws Exception {
    for (Endpoint e : endpoints()) {
      if (WITHOUT_RESOURCE.contains(e.path())) {
        continue;
      }
      int status;
      try {
        status = mvc.perform(request(e).with(user("matrix.admin@example.org"))).andReturn().getResponse().getStatus();
      } catch (jakarta.servlet.ServletException failedInThePage) {
        // past the access checks; the junk id and mid break some IPT 3 pages
        status = 500;
      }
      assertThat(status).as(e.toString()).isNotIn(401, 403);
    }
  }
}
