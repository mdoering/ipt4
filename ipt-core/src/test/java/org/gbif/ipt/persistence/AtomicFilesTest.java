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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtomicFilesTest {

  @TempDir
  Path dir;

  @Test
  void writesNewFilesAndCreatesParents() throws IOException {
    Path target = dir.resolve("a/b/resource.json");
    AtomicFiles.writeString(target, "{}");
    assertThat(Files.readString(target)).isEqualTo("{}");
  }

  @Test
  void replacesExistingFiles() throws IOException {
    Path target = dir.resolve("resource.json");
    Files.writeString(target, "old");
    AtomicFiles.writeString(target, "new");
    assertThat(Files.readString(target)).isEqualTo("new");
  }

  @Test
  void atomicWriteKeepsOldFileOnFailure() throws IOException {
    Path target = dir.resolve("resource.json");
    Files.writeString(target, "old content");

    assertThatThrownBy(() -> AtomicFiles.write(target, out -> {
      out.write("half writ".getBytes(StandardCharsets.UTF_8));
      throw new IOException("disk full");
    })).isInstanceOf(IOException.class).hasMessage("disk full");

    assertThat(Files.readString(target)).isEqualTo("old content");
    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(target);
    }
  }
}
