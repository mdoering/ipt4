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

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * {@code ipt migrate --from <IPT 3 data dir> --to <new IPT4 data dir> [--dry-run] [--link-artifacts] [--include-logs]}
 */
public class MigrateCommand {

  public static final int OK = 0;
  public static final int FAILED = 1;
  public static final int USAGE_ERROR = 2;

  private static final String USAGE = """
      Usage: java -jar ipt.jar migrate --from <IPT 3 data dir> --to <new IPT4 data dir> [options]
        --dry-run         read and check everything, write nothing
        --link-artifacts  hard-link published archives and versioned metadata instead of copying them
        --include-logs    copy the logs folder too
      The IPT 3 data dir is only read. The target must be empty or missing.""";

  private final PrintStream out;
  private final Map<String, String> env;

  public MigrateCommand(PrintStream out, Map<String, String> env) {
    this.out = out;
    this.env = env;
  }

  public int run(String... args) {
    Path from = null;
    Path to = null;
    boolean dryRun = false;
    boolean link = false;
    boolean logs = false;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--from" -> from = i + 1 < args.length ? Path.of(args[++i]) : null;
        case "--to" -> to = i + 1 < args.length ? Path.of(args[++i]) : null;
        case "--dry-run" -> dryRun = true;
        case "--link-artifacts" -> link = true;
        case "--include-logs" -> logs = true;
        default -> {
          out.println("Unknown option " + args[i]);
          out.println(USAGE);
          return USAGE_ERROR;
        }
      }
    }
    if (from == null || to == null) {
      out.println(USAGE);
      return USAGE_ERROR;
    }
    try {
      MigrationReport report = new Migrator(env).migrate(from, to, new MigrationOptions(dryRun, link, logs));
      out.print(report.toText());
      out.println(report.errors().isEmpty() ? "Migration finished." : "Migration finished with errors.");
      if (!dryRun) {
        out.println("Start IPT4 with IPT_DATA_DIR=" + to.toAbsolutePath().normalize());
      }
      return report.errors().isEmpty() ? OK : FAILED;
    } catch (MigrationException e) {
      out.println(e.getMessage());
      return FAILED;
    }
  }
}
