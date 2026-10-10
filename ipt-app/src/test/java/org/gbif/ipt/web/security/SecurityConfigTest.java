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
package org.gbif.ipt.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired MockMvc mvc;

  @Test
  void anonymousAdminRequestRedirectsToLogin() throws Exception {
    mvc.perform(get("/admin/")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
  }

  @Test
  void doSuffixedAdminUrlIsProtectedToo() throws Exception {
    mvc.perform(get("/admin/home.do")).andExpect(redirectedUrl("/login"));
  }

  @Test
  void loginRedirectKeepsContextPath() throws Exception {
    mvc.perform(get("/ipt/admin/").contextPath("/ipt")).andExpect(redirectedUrl("/ipt/login"));
  }

  @Test
  @WithMockUser(roles = "MANAGER")
  void managerCannotOpenAdmin() throws Exception {
    mvc.perform(get("/admin/")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminPassesSecurity() throws Exception {
    mvc.perform(get("/admin/nothing-here")).andExpect(status().isNotFound());
  }

  @Test
  void anonymousManageRequestRedirectsToLogin() throws Exception {
    mvc.perform(get("/manage/")).andExpect(redirectedUrl("/login"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void plainUserCannotManage() throws Exception {
    mvc.perform(get("/manage/")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "PUBLISHER")
  void publisherCanManage() throws Exception {
    mvc.perform(get("/manage/nothing-here")).andExpect(status().isNotFound());
  }

  @Test
  void postWithoutCsrfTokenIsForbidden() throws Exception {
    mvc.perform(post("/about")).andExpect(status().isForbidden());
  }

  @Test
  void postWithCsrfTokenPassesSecurity() throws Exception {
    MvcResult result = mvc.perform(post("/nothing-here").with(csrf())).andReturn();
    assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
  }

  @Test
  void apiV1IsStatelessAndWithoutCsrf() throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/jobs")).andReturn();
    assertThat(result.getResponse().getStatus()).isNotEqualTo(403);
    assertThat(result.getRequest().getSession(false)).isNull();
  }

  @Test
  void framesAreSameOriginOnly() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
  }

  @Test
  @WithMockUser
  void logoutIsAPostThatReturnsHome() throws Exception {
    mvc.perform(post("/logout").with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/"));
  }
}
