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
package org.gbif.ipt.legacy;

import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.persistence.SecretCipher;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceFolderImporterLegacyTest {

  @TempDir
  Path tmp;

  private Path unzip(String zip) throws Exception {
    try (ZipInputStream in = new ZipInputStream(getClass().getResourceAsStream("/ipt3/resources/" + zip))) {
      ZipEntry e;
      while ((e = in.getNextEntry()) != null) {
        Path target = tmp.resolve(e.getName()).normalize();
        if (e.isDirectory()) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(in, target);
        }
      }
    }
    return tmp.resolve("res1");
  }

  @Test
  void convertsAnIpt3ResourceFolderInPlace() throws Exception {
    Path dir = unzip("res1-ipt3.zip");
    ResourceFolderImporter converter =
        new ResourceFolderImporter(new SecretCipher(new byte[32]), JdbcSupport.fromClasspath());
    assertThat(converter.prepare(dir.toFile())).isEmpty();
    assertThat(dir.resolve("resource.xml")).doesNotExist();
    assertThat(dir.resolve("resource.json")).content().contains("\"extension\" : \"http://rs.tdwg.org/dwc/terms/Occurrence\"");
    assertThat(dir.resolve("eml.xml")).exists();
  }

  @Test
  void leavesOtherFoldersAlone() throws Exception {
    Files.writeString(tmp.resolve("resource.json"), "{}");
    ResourceFolderImporter converter =
        new ResourceFolderImporter(new SecretCipher(new byte[32]), JdbcSupport.fromClasspath());
    assertThat(converter.prepare(tmp.toFile())).isEmpty();
    assertThat(tmp.resolve("resource.json")).hasContent("{}");
  }
}
