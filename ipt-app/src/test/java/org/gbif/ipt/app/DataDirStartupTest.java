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
package org.gbif.ipt.app;

import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.persistence.DataDirFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataDirStartupTest {

  @TempDir
  Path tmp;

  @Test
  void refusesToStartWithoutADataDir() {
    assertThatThrownBy(() -> DataDirStartup.open(null, "4.0.0"))
        .isInstanceOf(DataDirStartup.DataDirStartupException.class)
        .hasMessageContaining("IPT_DATA_DIR");
    assertThatThrownBy(() -> DataDirStartup.open(" ", "4.0.0"))
        .isInstanceOf(DataDirStartup.DataDirStartupException.class);
  }

  @Test
  void setsUpAnEmptyDirectory() throws IOException {
    Path dir = tmp.resolve("new");
    DataDir dd = DataDirStartup.open(dir.toString(), "4.0.0");
    assertThat(dd.isConfigured()).isTrue();
    assertThat(dir.resolve("config/ipt.properties")).exists();
    assertThat(dir.resolve("resources")).isDirectory();
    assertThat(DataDirFormat.detect(dir)).isEqualTo(DataDirFormat.Kind.V4);
    assertThat(DataDirFormat.readMarker(dir).createdBy()).isEqualTo("4.0.0");
  }

  @Test
  void setsUpADirectoryHoldingOnlyHiddenFiles() throws IOException {
    Files.createFile(tmp.resolve(".DS_Store"));
    DataDirStartup.open(tmp.toString(), "4.0.0");
    assertThat(tmp.resolve("config/ipt.properties")).exists();
  }

  @Test
  void refusesIpt3DataDirsAndExplainsTheMigration() throws IOException {
    Files.createDirectories(tmp.resolve("config"));
    Files.writeString(tmp.resolve("config/ipt.properties"), "ipt.baseURL=http://x");
    assertThatThrownBy(() -> DataDirStartup.open(tmp.toString(), "4.0.0"))
        .isInstanceOf(DataDirStartup.DataDirStartupException.class)
        .hasMessageContaining("IPT 3")
        .hasMessageContaining("migrate --from " + tmp);
  }

  @Test
  void refusesHalfMigratedDataDirs() throws IOException {
    Files.createDirectories(tmp.resolve("config"));
    Files.writeString(tmp.resolve("config/ipt.properties"), "ipt.baseURL=http://x");
    Files.write(tmp.resolve("config/.secret.key"), new byte[32]);
    assertThatThrownBy(() -> DataDirStartup.open(tmp.toString(), "4.0.0"))
        .isInstanceOf(DataDirStartup.DataDirStartupException.class)
        .hasMessageContaining("did not finish");
  }

  @Test
  void refusesDirectoriesThatAreNoDataDir() throws IOException {
    Files.writeString(tmp.resolve("notes.txt"), "hello");
    assertThatThrownBy(() -> DataDirStartup.open(tmp.toString(), "4.0.0"))
        .isInstanceOf(DataDirStartup.DataDirStartupException.class)
        .hasMessageContaining("not an IPT data directory");
  }

  @Test
  void opensExistingIpt4DataDirs() throws IOException {
    DataDirStartup.open(tmp.toString(), "4.0.0");
    DataDir again = DataDirStartup.open(tmp.toString(), "4.0.1");
    assertThat(again.getDataDir()).isEqualTo(tmp.toFile());
    assertThat(DataDirFormat.readMarker(tmp).createdBy()).isEqualTo("4.0.0");
  }
}
