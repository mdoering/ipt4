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
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.servlet.MultipartConfigElement;

import static org.assertj.core.api.Assertions.assertThat;

/** Upload limits on a real server: nothing kept in memory, oversize files refused and not stored. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "ipt.upload.max-size=1KB")
class UploadLimitsTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @LocalServerPort int port;
  @Autowired MultipartConfigElement multipart;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;

  private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private static String token(String html) {
    return html.replaceAll("(?s).*name=\"_csrf\" content=\"([^\"]+)\".*", "$1");
  }

  @Test
  void partsAreNeverKeptInMemory() {
    assertThat(multipart.getFileSizeThreshold()).isZero();
    assertThat(multipart.getMaxFileSize()).isEqualTo(1024);
  }

  @Test
  void oversizeUploadsAreRefusedAndNotStored() throws Exception {
    User owner = ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    Resource res1 = resources.get("res1");
    if (!res1.getManagers().contains(owner)) {
      res1.addManager(owner);
    }
    String login = http.send(HttpRequest.newBuilder(URI.create(url("/login.do"))).build(),
        HttpResponse.BodyHandlers.ofString()).body();
    http.send(HttpRequest.newBuilder(URI.create(url("/login.do")))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("email=" + URLEncoder.encode(ManageFixture.OWNER, StandardCharsets.UTF_8)
            + "&password=" + ManageFixture.PASSWORD + "&_csrf=" + token(login.replace("name=\"_csrf\" value=",
            "name=\"_csrf\" content=")))).build(), HttpResponse.BodyHandlers.ofString());
    String overview = http.send(HttpRequest.newBuilder(URI.create(url("/manage/resource.do?r=res1"))).build(),
        HttpResponse.BodyHandlers.ofString()).body();
    assertThat(overview).contains("addSourceForm");

    String boundary = "----ipt4test";
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"r\"\r\n\r\nres1\r\n"
        + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"sourceType\"\r\n\r\nsource-file\r\n"
        + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"huge.txt\"\r\n"
        + "Content-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
    body.write(("id\tname\n" + "1\tx\n".repeat(2000)).getBytes(StandardCharsets.UTF_8));
    body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    HttpResponse<String> upload = http.send(HttpRequest.newBuilder(URI.create(url("/manage/addsource.do?_csrf="
            + token(overview))))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .header("X-Requested-With", "XMLHttpRequest")
        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
    assertThat(upload.statusCode()).isBetween(400, 499);
    assertThat(resources.get("res1").getSource("huge")).isNull();
    try (Stream<Path> files = Files.walk(dataDir)) {
      assertThat(files.filter(f -> f.getFileName().toString().startsWith("huge"))).isEmpty();
    }
  }
}
