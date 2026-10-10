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

import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/** The login form on a real server, where forwards and filters run as in production (MockMvc skips forwards). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginOverHttpTest {

  private static final String EMAIL = "http.user@example.org";
  private static final String PASSWORD = "Secret-pass-1";

  @LocalServerPort int port;
  @Autowired UserAccountManager users;

  private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager())
      .connectTimeout(Duration.ofSeconds(5)).build();

  @BeforeEach
  void user() throws Exception {
    if (users.get(EMAIL) == null) {
      User user = new User();
      user.setEmail(EMAIL);
      user.setFirstname("Http");
      user.setLastname("User");
      user.setRole(User.Role.Manager);
      user.setPassword(PASSWORD);
      users.create(user);
    }
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> postLogin(String form) throws Exception {
    String page = get("/login.do").body();
    String token = page.replaceAll("(?s).*name=\"_csrf\" value=\"([^\"]+)\".*", "$1");
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login.do"))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form + "&_csrf=" + token)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void wrongPasswordShowsTheFormAgain() throws Exception {
    long start = System.nanoTime();
    HttpResponse<String> failed = postLogin("email=" + URLEncoder.encode(EMAIL, StandardCharsets.UTF_8)
        + "&password=wrong-password");
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
    assertThat(failed.statusCode()).isEqualTo(302);
    String location = failed.headers().firstValue("Location").orElseThrow();
    HttpResponse<String> form = get(URI.create(location).getRawPath() + "?" + URI.create(location).getRawQuery());
    assertThat(form.statusCode()).isEqualTo(200);
    assertThat(form.body()).contains("The email – password combination does not exist.")
        .contains("value=\"" + EMAIL + "\"").doesNotContain("wrong-password");
  }

  /** The menu's Login button posts the CSRF token only; IPT 3 then showed the login form. */
  @Test
  void loginButtonWithoutCredentialsShowsTheForm() throws Exception {
    HttpResponse<String> form = postLogin("login=Login");
    assertThat(form.statusCode()).isEqualTo(200);
    assertThat(form.body()).contains("name=\"email\"").doesNotContain("combination does not exist");
  }

  @Test
  void rightPasswordLogsIn() throws Exception {
    HttpResponse<String> ok = postLogin("email=" + URLEncoder.encode(EMAIL, StandardCharsets.UTF_8)
        + "&password=" + PASSWORD);
    assertThat(ok.statusCode()).isEqualTo(302);
    assertThat(ok.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).doesNotContain("error"));
  }
}
