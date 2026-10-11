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
package org.gbif.ipt.web.support;

import org.gbif.ipt.app.ManageFixture;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's XssFieldErrorInterceptor: a flagged field is a form error, and nothing is saved. */
@SpringBootTest
@AutoConfigureMockMvc
class SanitizedFormsTest {

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired FilterRegistrationBean<SanitizeHtmlFilter> sanitizer;
  @Autowired DelegatingFilterProxyRegistrationBean securityFilterChain;

  /** Requests that security refuses (anonymous posts) are not parsed and sanitised for nothing. */
  @Test
  void sanitisingRunsAfterTheSecurityChain() {
    assertThat(sanitizer.getOrder()).isGreaterThan(securityFilterChain.getOrder());
  }

  @Test
  void htmlInAPlainFieldIsAFormError() throws Exception {
    ManageFixture.user(users, "sanitize@example.org", User.Role.User);
    MockHttpServletResponse r = mvc.perform(post("/account.do").with(user("sanitize@example.org")).with(csrf())
        .param("user.firstname", "<script>alert(1)</script>Eve").param("user.lastname", "Tester")
        .param("save", "Save")).andReturn().getResponse();
    assertThat(r.getStatus()).isEqualTo(200);
    assertThat(r.getContentAsString()).contains("Potentially malicious content was removed from this field")
        .doesNotContain("<script>alert(1)</script>");
    assertThat(users.get("sanitize@example.org").getFirstname()).isNotEqualTo("Eve");
  }
}
