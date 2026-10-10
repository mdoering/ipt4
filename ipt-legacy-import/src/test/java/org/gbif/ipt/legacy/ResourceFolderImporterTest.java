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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Resource folders exported from another IPT4 hold secrets encrypted with that installation's key. */
class ResourceFolderImporterTest {

  @TempDir
  Path dir;

  @Test
  void dropsSecretsThisInstallationCannotRead() throws Exception {
    byte[] otherKey = new byte[32];
    Arrays.fill(otherKey, (byte) 3);
    String foreign = new SecretCipher(otherKey).encrypt("db-secret");
    SecretCipher ours = new SecretCipher(new byte[32]);
    String readable = ours.encrypt("ok");
    Files.writeString(dir.resolve("resource.json"), """
        {
          "schemaVersion" : 1,
          "sources" : [ { "type" : "sql", "name" : "db", "password" : "%s", "rdbms" : "pgsql" },
                        { "type" : "sql", "name" : "local", "password" : "%s", "rdbms" : "pgsql" } ]
        }
        """.formatted(foreign, readable));

    ResourceFolderImporter importer = new ResourceFolderImporter(ours, JdbcSupport.fromClasspath());
    assertThat(importer.prepare(dir.toFile())).containsExactly("db");

    String json = Files.readString(dir.resolve("resource.json"));
    assertThat(json).doesNotContain(foreign).contains(readable);
  }

  @Test
  void leavesReadableFoldersUntouched() throws Exception {
    Files.writeString(dir.resolve("resource.json"), "{\"schemaVersion\":1}");
    ResourceFolderImporter importer = new ResourceFolderImporter(new SecretCipher(new byte[32]), JdbcSupport.fromClasspath());
    assertThat(importer.prepare(dir.toFile())).isEmpty();
    assertThat(dir.resolve("resource.json")).hasContent("{\"schemaVersion\":1}");
  }
}
