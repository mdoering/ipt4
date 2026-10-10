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
import org.gbif.ipt.model.SqlSource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** IPT 3's SourceAction: file, zipped and SQL sources, peek, delete, raw download. */
@SpringBootTest
@AutoConfigureMockMvc
class SourcesTest {

  private static final String DATA = "id\tscientificName\n1\tPuma concolor\n2\tLynx lynx\n";

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;

  @BeforeEach
  void owner() throws Exception {
    User owner = ManageFixture.user(users, ManageFixture.OWNER, User.Role.Manager);
    ManageFixture.user(users, ManageFixture.OTHER, User.Role.Manager);
    Resource res1 = resources.get("res1");
    if (!res1.getManagers().contains(owner)) {
      res1.addManager(owner);
    }
  }

  private MockHttpServletResponse as(String email, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
    return mvc.perform(request.with(user(email)).with(csrf())).andReturn().getResponse();
  }

  private MockHttpServletResponse addFile(String name, byte[] content) throws Exception {
    return as(ManageFixture.OWNER, multipart("/manage/addsource.do")
        .file(new MockMultipartFile("file", name, "text/plain", content))
        .param("r", "res1").param("sourceType", "source-file").param("validate", "false").param("add", "Add"));
  }

  @Test
  void addATextFile() throws Exception {
    MockHttpServletResponse r = addFile("plants.txt", DATA.getBytes(StandardCharsets.UTF_8));
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/source.do?r=res1&id=plants");
    assertThat(resources.get("res1").getSource("plants")).isNotNull();
    MockHttpServletResponse form = as(ManageFixture.OWNER, get("/manage/source.do?r=res1&id=plants"));
    assertThat(form.getStatus()).isEqualTo(200);
    assertThat(form.getContentAsString()).contains("plants");
    assertThat(as(ManageFixture.OWNER, get("/manage/peek.do?r=res1&id=plants")).getContentAsString())
        .contains("Lynx lynx");
    MockHttpServletResponse raw = as(ManageFixture.OWNER, get("/manage/raw-source.do?r=res1&id=plants"));
    assertThat(raw.getContentAsString()).isEqualTo(DATA);
    assertThat(as(ManageFixture.OTHER, get("/manage/raw-source.do?r=res1&id=plants")).getStatus()).isEqualTo(401);
    assertThat(as(ManageFixture.OTHER, get("/manage/peek.do?r=res1&id=plants")).getStatus()).isEqualTo(401);
  }

  @Test
  void addAZipOfTextFiles() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (String name : new String[]{"birds.txt", "fish.txt"}) {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(DATA.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    }
    MockHttpServletResponse r = addFile("animals.zip", bytes.toByteArray());
    assertThat(r.getRedirectedUrl()).startsWith("/manage/source.do?r=res1&id=");
    assertThat(resources.get("res1").getSource("birds")).isNotNull();
    assertThat(resources.get("res1").getSource("fish")).isNotNull();
  }

  @Test
  void hostileUploadNames() throws Exception {
    MockHttpServletResponse r = addFile("../../../evil.txt", DATA.getBytes(StandardCharsets.UTF_8));
    assertThat(r.getStatus()).isIn(302, 400);
    try (Stream<Path> files = Files.walk(dataDir.getParent())) {
      assertThat(files.filter(f -> f.getFileName().toString().startsWith("evil"))
          .filter(f -> !f.startsWith(dataDir.resolve("resources/res1/sources")))).isEmpty();
    }
  }

  @Test
  void anEmptyUploadIsABadRequest() throws Exception {
    MockHttpServletResponse r = as(ManageFixture.OWNER, multipart("/manage/addsource.do").param("r", "res1")
        .param("sourceType", "source-file").param("validate", "false").param("add", "Add"));
    assertThat(r.getStatus()).isEqualTo(400);
    assertThat(r.getHeader("X-Error-Message")).isNotBlank();
  }

  @Test
  void sqlPasswordNotRendered() throws Exception {
    MockHttpServletResponse added = as(ManageFixture.OWNER, post("/manage/addsource.do").param("r", "res1")
        .param("sourceType", "source-sql").param("sourceName", "db1").param("validate", "false").param("add", "Add"));
    assertThat(added.getRedirectedUrl()).isEqualTo("/manage/source.do?r=res1&id=db1");
    as(ManageFixture.OWNER, post("/manage/source.do").param("r", "res1").param("id", "db1")
        .param("source.name", "db1").param("rdbms", "pgsql").param("sqlSource.host", "127.0.0.1:9")
        .param("sqlSource.database", "ipt").param("sqlSource.username", "u").param("sqlSourcePassword", "TopSecret1")
        .param("sqlSource.sql", "select 1").param("save", "Save"));
    assertThat(((SqlSource) resources.get("res1").getSource("db1")).getPassword()).isEqualTo("TopSecret1");
    assertThat(as(ManageFixture.OWNER, get("/manage/source.do?r=res1&id=db1")).getContentAsString())
        .doesNotContain("TopSecret1");
    assertThat(Files.readString(dataDir.resolve("resources/res1/resource.json"))).doesNotContain("TopSecret1");
  }

  @Test
  void sourcesCannotBePointedAtServerFiles() throws Exception {
    addFile("trees.txt", DATA.getBytes(StandardCharsets.UTF_8));
    as(ManageFixture.OWNER, post("/manage/source.do").param("r", "res1").param("id", "trees")
        .param("source.name", "trees").param("source.file", "/etc/hosts").param("save", "Save"));
    assertThat(as(ManageFixture.OWNER, get("/manage/raw-source.do?r=res1&id=trees")).getContentAsString())
        .isEqualTo(DATA);
  }

  @Test
  void deleteASource() throws Exception {
    addFile("todelete.txt", DATA.getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse r = as(ManageFixture.OWNER, post("/manage/delete-source.do").param("r", "res1")
        .param("id", "todelete"));
    assertThat(r.getRedirectedUrl()).isEqualTo("/manage/resource.do?r=res1");
    assertThat(resources.get("res1").getSource("todelete")).isNull();
  }
}
