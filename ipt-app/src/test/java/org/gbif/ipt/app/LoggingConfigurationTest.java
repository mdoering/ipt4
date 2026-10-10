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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ipt.log-dir=target/logging-test/logs")
class LoggingConfigurationTest {

  @Test
  void writesIpt3StyleDebugAndAdminLogs() throws Exception {
    Logger log = LoggerFactory.getLogger("org.gbif.ipt.logging.probe");
    String marker = UUID.randomUUID().toString();

    log.debug("debug {}", marker);
    log.warn("warn {}", marker);

    Path dir = Path.of("target/logging-test/logs");
    String debugLog = Files.readString(dir.resolve("debug.log"));
    String adminLog = Files.readString(dir.resolve("admin.log"));

    assertThat(debugLog).contains("debug " + marker).contains("warn " + marker);
    assertThat(adminLog).contains("warn " + marker).doesNotContain("debug " + marker);
    assertThat(adminLog)
        .containsPattern(
            "WARN  \\d{2}-\\S{3,4}-\\d{4} \\d{2}:\\d{2}:\\d{2} \\[org\\.gbif\\.ipt\\.logging\\.probe\\] - warn " + marker);
  }
}
