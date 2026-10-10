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
package org.gbif.ipt;

import org.gbif.ipt.legacy.MigrateCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IptApplicationCommandTest {

  @Test
  void migrateRunsWithoutStartingTheWebApplication() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    var code = IptApplication.runCommand(new String[] {"migrate"}, new PrintStream(out, true, StandardCharsets.UTF_8));
    assertThat(code).hasValue(MigrateCommand.USAGE_ERROR);
    assertThat(out.toString(StandardCharsets.UTF_8)).contains("migrate --from");
  }

  @Test
  void anythingElseStartsTheWebApplication() {
    assertThat(IptApplication.runCommand(new String[] {}, System.out)).isEmpty();
    assertThat(IptApplication.runCommand(new String[] {"--server.port=9000"}, System.out)).isEmpty();
  }
}
