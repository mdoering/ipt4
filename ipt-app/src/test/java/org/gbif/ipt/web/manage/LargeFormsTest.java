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
import org.gbif.ipt.feedback.CollectingFeedback;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.io.File;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** A contacts form with thousands of fields on a real server (Tomcat's parameter limits, binder auto-grow). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LargeFormsTest {

  private static final int CONTACTS = 600;

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @LocalServerPort int port;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;

  private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, List<String[]> fields) throws Exception {
    StringBuilder body = new StringBuilder();
    for (String[] f : fields) {
      if (!body.isEmpty()) {
        body.append('&');
      }
      body.append(URLEncoder.encode(f[0], StandardCharsets.UTF_8)).append('=')
          .append(URLEncoder.encode(f[1], StandardCharsets.UTF_8));
    }
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void thousandsOfContactFields() throws Exception {
    User owner = ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    if (resources.get("big") == null) {
      File eml = Files.createTempFile("eml", ".xml").toFile();
      try (InputStream in = getClass().getResourceAsStream("/create/eml.xml")) {
        Files.copy(in, eml.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      resources.create("big", "occurrence", eml, owner, new CollectingFeedback(null, owner));
    }
    String login = get("/login.do").body();
    post("/login.do", List.of(new String[]{"email", ManageFixture.OWNER}, new String[]{"password", ManageFixture.PASSWORD},
        new String[]{"_csrf", Jsoup.parse(login).selectFirst("input[name=_csrf]").val()}));

    Element form = Jsoup.parse(get("/manage/metadata-contacts.do?r=big").body()).selectFirst("form[action*=contacts]");
    assertThat(form).isNotNull();
    List<String[]> fields = new ArrayList<>();
    List<String[]> firstContact = new ArrayList<>();
    for (Element field : form.select("input[name], select[name], textarea[name]")) {
      String name = field.attr("name");
      String type = field.attr("type");
      if (type.equals("submit") || type.equals("button") || ((type.equals("checkbox") || type.equals("radio"))
          && !field.hasAttr("checked"))) {
        continue;
      }
      String value = field.tagName().equals("select")
          ? (field.selectFirst("option[selected]") != null ? field.selectFirst("option[selected]").val() : "")
          : field.tagName().equals("textarea") ? field.wholeText() : field.val();
      if (name.startsWith("eml.contacts[0].")) {
        firstContact.add(new String[]{name, value});
      } else if (!name.startsWith("eml.contacts[")) {
        fields.add(new String[]{name, value});
      }
    }
    assertThat(firstContact).isNotEmpty();
    for (int i = 0; i < CONTACTS; i++) {
      for (String[] f : firstContact) {
        fields.add(new String[]{f[0].replace("eml.contacts[0].", "eml.contacts[" + i + "]."),
            f[0].endsWith("lastName") ? "Contact " + i : f[1]});
      }
    }
    fields.add(new String[]{"save", "Save"});
    assertThat(fields).hasSizeGreaterThan(5000);

    HttpResponse<String> saved = post(form.attr("action").startsWith("/") ? form.attr("action")
        : "/manage/" + form.attr("action"), fields);
    assertThat(saved.statusCode()).as(saved.body().length() + " bytes").isEqualTo(302);
    assertThat(resources.get("big").getEml().getContacts()).hasSize(CONTACTS);
    assertThat(resources.get("big").getEml().getContacts().get(CONTACTS - 1).getLastName())
        .isEqualTo("Contact " + (CONTACTS - 1));
  }
}
