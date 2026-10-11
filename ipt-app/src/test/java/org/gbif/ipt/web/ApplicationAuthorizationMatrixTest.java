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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Every handler of the application, enumerated from the request mappings: a state-changing request without the CSRF
 * token is refused for everyone, and the protected areas send anonymous visitors to the login. The role matrices of
 * the manage and admin areas are in their own tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationAuthorizationMatrixTest {

  private static final String ADMIN = "app.matrix.admin@example.org";
  private static final List<String> PROTECTED = List.of("/manage", "/manager-api", "/admin", "/admin-api", "/account");

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
  void admin() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
  }

  private List<Endpoint> endpoints() {
    List<Endpoint> endpoints = new ArrayList<>();
    for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
      Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
      for (String pattern : info.getPatternValues()) {
        if (pattern.startsWith("/_probe") || pattern.startsWith("/error")) {
          continue;
        }
        String path = pattern.replaceAll("\\{[^}]+}", "x");
        for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET, RequestMethod.POST) : methods) {
          endpoints.add(new Endpoint(path, method));
        }
      }
    }
    assertThat(endpoints).hasSizeGreaterThan(100);
    return endpoints;
  }

  private static boolean changing(RequestMethod method) {
    return method == RequestMethod.POST || method == RequestMethod.PUT || method == RequestMethod.DELETE
        || method == RequestMethod.PATCH;
  }

  @Test
  void everyStateChangingRequestNeedsTheCsrfToken() throws Exception {
    List<String> accepted = new ArrayList<>();
    for (Endpoint e : endpoints()) {
      if (!changing(e.method())) {
        continue;
      }
      var request = switch (e.method()) {
        case PUT -> put(e.path());
        case DELETE -> delete(e.path());
        default -> post(e.path());
      };
      for (boolean asAdmin : new boolean[] {false, true}) {
        int status = mvc.perform(asAdmin ? request.with(user(ADMIN)) : request).andReturn().getResponse().getStatus();
        if (status != 403) {
          accepted.add(e + (asAdmin ? " as admin" : " anonymously") + " -> " + status);
        }
      }
    }
    assertThat(accepted).isEmpty();
  }

  @Test
  void protectedAreasSendAnonymousVisitorsToTheLogin() throws Exception {
    List<String> open = new ArrayList<>();
    for (Endpoint e : endpoints()) {
      if (e.method() != RequestMethod.GET || PROTECTED.stream().noneMatch(p -> e.path().startsWith(p))) {
        continue;
      }
      String location = mvc.perform(get(e.path())).andReturn().getResponse().getRedirectedUrl();
      if (location == null || !location.endsWith("/login")) {
        open.add(e.path() + " -> " + location);
      }
    }
    assertThat(open).isEmpty();
  }
}
