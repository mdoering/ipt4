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
import org.gbif.ipt.web.BrowserForms;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** IPT 3's UserAccountsAction. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminUsersTest {

  private static final String ADMIN = "users.admin@example.org";
  private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;

  @BeforeEach
  void admin() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
  }

  private String page(String url, String as) throws Exception {
    MockHttpServletResponse response = mvc.perform(get(URI.create(url)).with(user(as))).andReturn().getResponse();
    assertThat(response.getStatus()).as(url).isEqualTo(200);
    assertThat(response.getContentAsString()).as(url).doesNotContain("FreeMarker template error");
    return response.getContentAsString();
  }

  private MvcResult submit(String url, Map<String, String> changed) throws Exception {
    return mvc.perform(BrowserForms.submit(Jsoup.parse(page(url, ADMIN), "http://localhost" + url), "user.do", null,
        "", changed).with(user(ADMIN)).with(csrf())).andReturn();
  }

  private MvcResult create(String email, String role, String password, String password2) throws Exception {
    Map<String, String> fields = new HashMap<>(Map.of("user.email", email, "user.firstname", "Ann",
        "user.lastname", "Lee", "user.role", role, "user.password", password, "password2", password2));
    return submit("/admin/user.do", fields);
  }

  @Test
  void listShowsTheUsers() throws Exception {
    assertThat(page("/admin/users.do", ADMIN)).contains(ADMIN, ManageFixture.OWNER);
  }

  @Test
  void createAUser() throws Exception {
    MvcResult created = create("new.user@example.org", "Manager", "Secret-pass-2", "Secret-pass-2");
    assertThat(created.getResponse().getRedirectedUrl()).isEqualTo("/admin/users.do");
    User user = users.get("new.user@example.org");
    assertThat(user.getRole()).isEqualTo(User.Role.Manager);
    assertThat(BCRYPT.matches("Secret-pass-2", user.getPassword())).isTrue();
  }

  @Test
  void passwordsMustMatchAndEmailsBeNew() throws Exception {
    MvcResult mismatch = create("mismatch@example.org", "User", "Secret-pass-2", "Other-pass-2");
    assertThat(org.jsoup.Jsoup.parse(mismatch.getResponse().getContentAsString()).text())
        .contains("The passwords provided don't match.");
    assertThat(users.get("mismatch@example.org")).isNull();

    MvcResult existing = create(ManageFixture.OWNER, "User", "Secret-pass-2", "Secret-pass-2");
    assertThat(existing.getResponse().getContentAsString())
        .contains("A user with the email address " + ManageFixture.OWNER + " already exists.");
    assertThat(users.get(ManageFixture.OWNER).getRole()).isEqualTo(User.Role.Manager);
  }

  @Test
  void editKeepsEmailAndPassword() throws Exception {
    User other = ManageFixture.user(users, "edit.me@example.org", User.Role.User);
    String hash = other.getPassword();
    MvcResult saved = submit("/admin/user.do?id=edit.me@example.org", Map.of("user.lastname", "Changed",
        "user.role", "Publisher", "user.email", "hijack@example.org", "user.password", "plain"));
    assertThat(saved.getResponse().getRedirectedUrl()).isEqualTo("/admin/users.do");
    User edited = users.get("edit.me@example.org");
    assertThat(edited.getLastname()).isEqualTo("Changed");
    assertThat(edited.getRole()).isEqualTo(User.Role.Publisher);
    assertThat(edited.getPassword()).isEqualTo(hash);
    assertThat(users.get("hijack@example.org")).isNull();
  }

  @Test
  void rejectedUserFormChangesNothing() throws Exception {
    ManageFixture.user(users, "keep.me@example.org", User.Role.User);
    MvcResult rejected = submit("/admin/user.do?id=keep.me@example.org",
        Map.of("user.firstname", "", "user.lastname", "", "user.role", "Admin"));
    assertThat(rejected.getResponse().getRedirectedUrl()).isNull();
    User kept = users.get("keep.me@example.org");
    assertThat(kept.getLastname()).isEqualTo("Tester");
    assertThat(kept.getRole()).isEqualTo(User.Role.User);
  }

  @Test
  void resetPassword() throws Exception {
    String hash = ManageFixture.user(users, "reset.me@example.org", User.Role.User).getPassword();
    MvcResult reset = submit("/admin/user.do?id=reset.me@example.org", Map.of("resetPassword", "Reset"));
    assertThat(reset.getResponse().getRedirectedUrl()).isEqualTo("/admin/users.do");
    assertThat(users.get("reset.me@example.org").getPassword()).isNotEqualTo(hash).startsWith("$2a$12$");
  }

  @Test
  void adminsCannotDeleteThemselvesOrDemoteTheLastAdmin() throws Exception {
    MvcResult self = submit("/admin/user.do?id=" + ADMIN, Map.of("deleteFlag", "Delete"));
    assertThat(self.getResponse().getContentAsString()).contains("One cannot remove an account when logged in to it.");
    assertThat(users.get(ADMIN)).isNotNull();

    // the other admins become managers, then the last one cannot give up the role
    for (User admin : users.list(User.Role.Admin)) {
      if (!admin.getEmail().equals(ADMIN)) {
        submit("/admin/user.do?id=" + admin.getEmail(), Map.of("user.role", "Manager"));
      }
    }
    assertThat(users.list(User.Role.Admin)).extracting(User::getEmail).containsExactly(ADMIN);
    MvcResult demoted = submit("/admin/user.do?id=" + ADMIN, Map.of("user.role", "Manager"));
    assertThat(demoted.getResponse().getContentAsString()).contains("You cannot change the role of the last administrator.");
    assertThat(users.get(ADMIN).getRole()).isEqualTo(User.Role.Admin);
  }

  @Test
  void demotedAdminLosesAccess() throws Exception {
    ManageFixture.user(users, "second.admin@example.org", User.Role.Admin);
    assertThat(mvc.perform(get("/admin/").with(user("second.admin@example.org"))).andReturn().getResponse()
        .getStatus()).isEqualTo(200);
    submit("/admin/user.do?id=second.admin@example.org", Map.of("user.role", "Manager"));
    assertThat(mvc.perform(get("/admin/").with(user("second.admin@example.org"))).andReturn().getResponse()
        .getStatus()).isEqualTo(401);
  }

  /** The IPT 3 fixture's res1 was created by kbraak@gbif.org. */
  @Test
  void theCreatorOfAResourceIsKept() throws Exception {
    MvcResult refused = submit("/admin/user.do?id=kbraak@gbif.org", Map.of("deleteFlag", "Delete"));
    assertThat(refused.getResponse().getContentAsString()).contains("res1");
    assertThat(users.get("kbraak@gbif.org")).isNotNull();
  }
}
