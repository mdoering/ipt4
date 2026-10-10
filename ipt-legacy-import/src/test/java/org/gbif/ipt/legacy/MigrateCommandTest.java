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

import org.gbif.ipt.persistence.DataDirFormat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class MigrateCommandTest {

  @TempDir
  Path tmp;

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  private int run(String... args) {
    return new MigrateCommand(new PrintStream(out, true, StandardCharsets.UTF_8), Map.of()).run(args);
  }

  @Test
  void printsUsageOnBadArguments() {
    assertThat(run()).isEqualTo(MigrateCommand.USAGE_ERROR);
    assertThat(run("--from", "x")).isEqualTo(MigrateCommand.USAGE_ERROR);
    assertThat(run("--from", "x", "--to", "y", "--bogus")).isEqualTo(MigrateCommand.USAGE_ERROR);
    assertThat(out.toString(StandardCharsets.UTF_8)).contains("migrate --from");
  }

  @Test
  void migrates() throws Exception {
    Path from = Ipt3Fixtures.dataDir(tmp.resolve("ipt3"));
    Path to = tmp.resolve("ipt4");
    assertThat(run("--from", from.toString(), "--to", to.toString())).isZero();
    assertThat(DataDirFormat.detect(to)).isEqualTo(DataDirFormat.Kind.V4);
    assertThat(out.toString(StandardCharsets.UTF_8)).contains("3 resources").contains("Migration finished");
  }

  @Test
  void failsWithMessageOnRefusal() throws Exception {
    assertThat(run("--from", tmp.resolve("missing").toString(), "--to", tmp.resolve("x").toString()))
        .isEqualTo(MigrateCommand.FAILED);
    assertThat(out.toString(StandardCharsets.UTF_8)).contains("IPT 3");
  }
}
