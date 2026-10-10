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
package org.gbif.ipt.web.account;

import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.nio.file.Files;
import java.nio.file.Path;

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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's LoginAction and AccountAction on Spring Security form login. */
@SpringBootTest
@AutoConfigureMockMvc
class LoginTest {

  private static final String EMAIL = "manager@example.org";
  private static final String PASSWORD = "Secret-pass-1";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = PortalFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired AppConfig cfg;

  @BeforeEach
  void manager() throws Exception {
    User user = users.get(EMAIL);
    if (user == null) {
      user = new User();
      user.setEmail(EMAIL);
      user.setFirstname("Maria");
      user.setLastname("Manager");
      user.setRole(User.Role.Manager);
      user.setPassword(PASSWORD);
      users.create(user);
    }
  }

  private MvcResult login(MockHttpSession session, String email, String password) throws Exception {
    return mvc.perform(post("/login.do").session(session).param("email", email).param("password", password)
        .with(csrf())).andReturn();
  }

  @Test
  void loginSucceedsRotatesTheSessionAndRecordsTheLogin() throws Exception {
    MockHttpSession session = new MockHttpSession();
    String before = session.getId();
    MvcResult result = login(session, " " + EMAIL + " ", PASSWORD + " ");
    MockHttpServletResponse response = result.getResponse();
    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getRedirectedUrl()).isEqualTo("/");
    assertThat(result.getRequest().getSession().getId()).isNotEqualTo(before);
    assertThat(result.getRequest().getSession().getMaxInactiveInterval()).isEqualTo(cfg.getSessionTimeout());
    assertThat(users.get(EMAIL).getLastLogin()).isNotNull();
    assertThat(Files.readString(dataDir.resolve("config/users.json"))).contains("lastLogin");
  }

  /** Spring Security forwards a failed login to the form (MockMvc records the forward without following it). */
  @Test
  void wrongPasswordShowsTheLoginPageAgain() throws Exception {
    assertThat(login(new MockHttpSession(), EMAIL, "wrong-password").getResponse().getForwardedUrl())
        .isEqualTo("/login?error");
    MockHttpServletResponse form = mvc.perform(get("/login").param("error", "").param("email", EMAIL)
        .param("password", "wrong-password")).andReturn().getResponse();
    assertThat(form.getStatus()).isEqualTo(200);
    assertThat(form.getContentAsString())
        .contains("The email – password combination does not exist.")
        .contains("value=\"" + EMAIL + "\"")
        .doesNotContain("wrong-password");
  }

  @Test
  void loginWithoutCsrfTokenIsRejected() throws Exception {
    assertThat(mvc.perform(post("/login").param("email", EMAIL).param("password", PASSWORD)).andReturn()
        .getResponse().getStatus()).isEqualTo(403);
  }

  @Test
  void loginReturnsToTheProtectedPageThatAskedForIt() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mvc.perform(get("/manage/resource.do?r=res1").session(session)).andReturn();
    // the .do suffix is gone once DoSuffixFilter has seen the request; both forms work
    assertThat(login(session, EMAIL, PASSWORD).getResponse().getRedirectedUrl())
        .matches("/manage/resource(\\.do)?\\?r=res1");
  }

  @Test
  void loginReturnsToTheReferringPage() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mvc.perform(get("/login.do").session(session).header("Referer", "http://localhost:8080/resource?r=res1"))
        .andReturn();
    assertThat(login(session, EMAIL, PASSWORD).getResponse().getRedirectedUrl())
        .isEqualTo("/resource?r=res1");
  }

  @Test
  void loginNeverRedirectsOffSite() throws Exception {
    for (String referer : new String[]{"https://evil.example.org/x", "http://localhost:8080.evil.example.org/x",
        "http://localhost:8080@evil.example.org/x", "http://localhost:8080//evil.example.org/x",
        "http://localhost:8080/\\evil.example.org/x", "http://localhost:8080/\t/evil.example.org/x"}) {
      MockHttpSession session = new MockHttpSession();
      mvc.perform(get("/login").session(session).header("Referer", referer)).andReturn();
      MockHttpServletResponse response = mvc.perform(post("/login").session(session).param("email", EMAIL)
          .param("password", PASSWORD).param("redirectUrl", "https://evil.example.org/").with(csrf())).andReturn()
          .getResponse();
      String location = response.getRedirectedUrl();
      assertThat(location).as(referer).startsWith("/").doesNotStartWith("//").doesNotStartWith("/\\")
          .doesNotContain("\t");
    }
  }

  @Test
  void loggedInUsersAreSentOnFromTheLoginPage() throws Exception {
    MockHttpSession session = new MockHttpSession();
    login(session, EMAIL, PASSWORD);
    assertThat(mvc.perform(get("/login").session(session)).andReturn().getResponse().getStatus()).isEqualTo(302);
  }

  @Test
  void logoutEndsTheSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    MvcResult loggedIn = login(session, EMAIL, PASSWORD);
    MockHttpSession current = (MockHttpSession) loggedIn.getRequest().getSession();
    MvcResult out = mvc.perform(post("/logout").session(current).with(csrf())).andReturn();
    assertThat(out.getResponse().getRedirectedUrl()).isEqualTo("/");
    assertThat(current.isInvalid()).isTrue();
  }

  @Test
  void roleChangesApplyWithoutLoggingInAgain() throws Exception {
    MockHttpSession session = (MockHttpSession) login(new MockHttpSession(), EMAIL, PASSWORD).getRequest()
        .getSession();
    assertThat(mvc.perform(get("/admin/").session(session)).andReturn().getResponse().getStatus()).isNotIn(200, 404);
    users.get(EMAIL).setRole(User.Role.Admin);
    try {
      assertThat(mvc.perform(get("/admin/nothing-here").session(session)).andReturn().getResponse().getStatus())
          .isEqualTo(404);
    } finally {
      users.get(EMAIL).setRole(User.Role.Manager);
    }
  }

  @Test
  void accountNeedsALogin() throws Exception {
    assertThat(mvc.perform(get("/account.do")).andReturn().getResponse().getRedirectedUrl()).endsWith("/login");
  }

  @Test
  void accountUpdatesNamesButNotTheRole() throws Exception {
    MockHttpSession session = (MockHttpSession) login(new MockHttpSession(), EMAIL, PASSWORD).getRequest()
        .getSession();
    assertThat(mvc.perform(get("/account").session(session)).andReturn().getResponse().getContentAsString())
        .contains("Maria");
    MockHttpServletResponse saved = mvc.perform(post("/account.do").session(session).with(csrf())
        .param("user.firstname", "Mia").param("user.lastname", "Manager").param("user.role", "Admin")
        .param("save", "Save")).andReturn().getResponse();
    assertThat(saved.getRedirectedUrl()).endsWith("/account.do");
    assertThat(users.get(EMAIL).getFirstname()).isEqualTo("Mia");
    assertThat(users.get(EMAIL).getRole()).isEqualTo(User.Role.Manager);
    assertThat(mvc.perform(get("/account.do").session(session)).andReturn().getResponse().getContentAsString())
        .contains("Your user account was successfully updated.");
    users.get(EMAIL).setFirstname("Maria");
  }

  @Test
  void passwordChange() throws Exception {
    MockHttpSession session = (MockHttpSession) login(new MockHttpSession(), EMAIL, PASSWORD).getRequest()
        .getSession();
    MockHttpServletResponse wrong = mvc.perform(post("/change-password.do").session(session).with(csrf())
        .param("currentPassword", "nope").param("newPassword", "Another-pass-2").param("password2", "Another-pass-2"))
        .andReturn().getResponse();
    assertThat(wrong.getStatus()).isEqualTo(200);
    assertThat(wrong.getContentAsString()).contains("The password provided doesn");

    MockHttpServletResponse mismatch = mvc.perform(post("/change-password.do").session(session).with(csrf())
        .param("currentPassword", PASSWORD).param("newPassword", "Another-pass-2").param("password2", "Other-3"))
        .andReturn().getResponse();
    assertThat(mismatch.getContentAsString()).contains("The passwords provided don");

    MockHttpServletResponse changed = mvc.perform(post("/change-password.do").session(session).with(csrf())
        .param("currentPassword", PASSWORD).param("newPassword", "Another-pass-2").param("password2", "Another-pass-2"))
        .andReturn().getResponse();
    assertThat(changed.getRedirectedUrl()).endsWith("/account.do");
    try {
      assertThat(users.authenticate(EMAIL, "Another-pass-2")).isNotNull();
      // IPT 3 only kept the new password in memory until the next save of users
      assertThat(Files.readString(dataDir.resolve("config/users.json"))).doesNotContain(PASSWORD)
          .contains(users.get(EMAIL).getPassword());
    } finally {
      users.get(EMAIL).setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12)
          .encode(PASSWORD));
      users.save();
    }
  }
}
