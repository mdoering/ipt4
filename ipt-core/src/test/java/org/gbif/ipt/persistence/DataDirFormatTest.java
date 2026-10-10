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
package org.gbif.ipt.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class DataDirFormatTest {

  @TempDir
  Path dir;

  @Test
  void missingOrEmptyDirectoriesAreEmpty() throws IOException {
    assertThat(DataDirFormat.detect(dir.resolve("nope"))).isEqualTo(DataDirFormat.Kind.EMPTY);
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.EMPTY);
    Files.createFile(dir.resolve(".DS_Store"));
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.EMPTY);
  }

  @Test
  void recognisesIpt3DataDirs() throws IOException {
    Files.createDirectories(dir.resolve("config"));
    Files.writeString(dir.resolve("config/ipt.properties"), "ipt.baseURL=http://x");
    Files.writeString(dir.resolve("config/users.xml"), "<users/>");
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.IPT3);
  }

  @Test
  void markerMakesItAnIpt4DataDir() throws IOException {
    DataDirFormat.writeMarker(dir, "4.0.0-SNAPSHOT", null, "check");
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.V4);
    assertThat(DataDirFormat.readMarker(dir).version()).isEqualTo(DataDirFormat.CURRENT_VERSION);
    assertThat(Files.readString(dir.resolve(DataDirFormat.MARKER))).contains("\"format\" : \"ipt\"");
    assertThat(DataDirFormat.readMarker(dir).keyCheck()).isEqualTo("check");
  }

  @Test
  void anInterruptedMigrationIsIncomplete() throws IOException {
    Files.createDirectories(dir.resolve("config"));
    Files.writeString(dir.resolve("config/ipt.properties"), "ipt.baseURL=http://x");
    Files.write(dir.resolve("config/" + SecretCipher.KEY_FILE), new byte[32]);
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.INCOMPLETE);
  }

  @Test
  void anythingElseIsUnknown() throws IOException {
    Files.writeString(dir.resolve("random.txt"), "hello");
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.UNKNOWN);
  }
}
