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
package org.gbif.ipt.web.error;

import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IPT 3's error results (error/404, error/401, error/401-manager, error/error) with their status codes, for errors
 * raised anywhere: unknown URLs, failing pages, missing rights. On a real server, as errors go through the container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("error-probe")
class ErrorPagesTest {

  private static final String USER = "plain.user@example.org";
  private static final String PASSWORD = "Secret-pass-1";

  @LocalServerPort int port;
  @Autowired UserAccountManager users;

  private final HttpClient http = HttpClient.newBuilder().cookieHandler(new java.net.CookieManager()).build();

  @BeforeEach
  void plainUser() throws Exception {
    if (users.get(USER) == null) {
      User user = new User();
      user.setEmail(USER);
      user.setFirstname("Plain");
      user.setLastname("User");
      user.setRole(User.Role.User);
      user.setPassword(PASSWORD);
      users.create(user);
    }
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private void loginAsPlainUser() throws Exception {
    String form = get("/login.do").body();
    String token = form.replaceAll("(?s).*name=\"_csrf\" value=\"([^\"]+)\".*", "$1");
    HttpResponse<String> login = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login.do"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("email=" + USER + "&password=" + PASSWORD + "&_csrf=" + token))
            .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(login.statusCode()).isEqualTo(302);
  }

  @Test
  void unknownPagesAreNotFound() throws Exception {
    for (String path : new String[]{"/nothing-here", "/nothing-here.do", "/styles/nothing.css"}) {
      HttpResponse<String> r = get(path);
      assertThat(r.statusCode()).as(path).isEqualTo(404);
      assertThat(r.body()).as(path).contains("<title>Page Not Found").contains("</html>");
    }
  }

  @Test
  void failingPagesShowTheErrorPage() throws Exception {
    HttpResponse<String> r = get("/_error/fail");
    assertThat(r.statusCode()).isEqualTo(500);
    assertThat(r.body()).contains("Probe failure &lt;script&gt;").doesNotContain("Probe failure <script>")
        .contains("</html>");
  }

  @Test
  void usersWithoutRightsGetIpt3s401Pages() throws Exception {
    loginAsPlainUser();
    HttpResponse<String> admin = get("/admin/");
    assertThat(admin.statusCode()).isEqualTo(401);
    assertThat(admin.body()).contains("<title>Not Authorized");
    HttpResponse<String> manage = get("/manage/");
    assertThat(manage.statusCode()).isEqualTo(401);
    assertThat(manage.body()).contains("Manager Login Required");
  }

  @Test
  void anonymousUsersAreStillSentToLogin() throws Exception {
    HttpResponse<String> r = get("/manage/");
    assertThat(r.statusCode()).isEqualTo(302);
    assertThat(r.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).endsWith("/login"));
  }
}
