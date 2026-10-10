package org.gbif.ipt.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

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
