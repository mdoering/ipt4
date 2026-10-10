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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.OptionalInt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Lives in the root package so every test below {@code org.gbif.ipt} finds it as its Spring Boot configuration. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IptApplication {

  public static void main(String[] args) {
    OptionalInt exitCode = runCommand(args, System.out);
    if (exitCode.isPresent()) {
      System.exit(exitCode.getAsInt());
    }
    SpringApplication.run(IptApplication.class, args);
  }

  /**
   * Runs a command-line tool instead of the web application: {@code migrate} converts an IPT 3 data dir.
   *
   * @return the exit code, or empty when the web application should start
   */
  static OptionalInt runCommand(String[] args, PrintStream out) {
    if (args.length > 0 && "migrate".equals(args[0])) {
      return OptionalInt.of(new MigrateCommand(out, System.getenv()).run(Arrays.copyOfRange(args, 1, args.length)));
    }
    return OptionalInt.empty();
  }
}
