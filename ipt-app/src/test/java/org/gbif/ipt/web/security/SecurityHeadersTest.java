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

import org.gbif.ipt.app.ManageFixture;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Response headers and the session cookie as a browser receives them (over real HTTP). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityHeadersTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @LocalServerPort int port;

  private HttpResponse<String> get(String path) throws Exception {
    return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void pagesSendTheSecurityHeaders() throws Exception {
    HttpResponse<String> login = get("/login.do");
    assertThat(login.statusCode()).isEqualTo(200);
    assertThat(login.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    assertThat(login.headers().firstValue("X-Frame-Options")).hasValue("SAMEORIGIN");
  }

  /** The login form's CSRF token starts the session: its cookie is HttpOnly and not sent by other sites' posts. */
  @Test
  void theSessionCookieIsHttpOnlyAndSameSite() throws Exception {
    String cookie = get("/login.do").headers().allValues("Set-Cookie").stream()
        .filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElseThrow();
    assertThat(cookie).containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Lax");
  }
}
