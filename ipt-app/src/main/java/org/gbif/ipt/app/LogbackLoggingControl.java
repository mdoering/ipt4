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

import org.gbif.ipt.config.LoggingControl;

import java.io.File;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

/**
 * Switches the IPT's own loggers between INFO and DEBUG when the admin changes the debug setting. The log files
 * themselves always live in the data dir (see {@code logback-spring.xml}).
 */
public class LogbackLoggingControl implements LoggingControl {

  private final LoggingSystem loggingSystem;

  public LogbackLoggingControl(LoggingSystem loggingSystem) {
    this.loggingSystem = loggingSystem;
  }

  @Override
  public void apply(File logDir, boolean debug) {
    loggingSystem.setLogLevel("org.gbif", debug ? LogLevel.DEBUG : LogLevel.INFO);
  }
}
