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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the data dir at start-up. IPT4 only starts on its own data dirs: an empty or missing directory is set up, an
 * IPT 3 data dir must first be converted with {@code ipt migrate}, anything else is refused.
 */
public final class DataDirStartup {

  private static final Logger LOG = LoggerFactory.getLogger(DataDirStartup.class);

  /** The data dir cannot be used; the message tells the operator what to do. */
  public static class DataDirStartupException extends IllegalStateException {
    public DataDirStartupException(String message) {
      super(message);
    }

    public DataDirStartupException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private DataDirStartup() {
  }

  public static DataDir open(@Nullable String location, String version) {
    if (location == null || location.isBlank()) {
      throw new DataDirStartupException("No IPT data directory configured. Set the environment variable IPT_DATA_DIR"
          + " (or the property ipt.data-dir) to a directory the IPT may write to.");
    }
    Path dir = Path.of(location).toAbsolutePath().normalize();
    try {
      DataDirFormat.Kind kind = DataDirFormat.detect(dir);
      switch (kind) {
        case EMPTY -> {
          LOG.info("Setting up a new IPT data directory in {}", dir);
          DataDir dd = DataDir.buildFromString(dir.toString());
          dd.setDataDir(dir.toFile());
          DataDirFormat.writeMarker(dir, version, null);
          return dd;
        }
        case V4 -> {
          LOG.info("Using IPT data directory {}", dir);
          DataDir dd = DataDir.buildFromString(dir.toString());
          clearTmp(dd);
          return dd;
        }
        case IPT3 -> throw new DataDirStartupException("The data directory " + dir + " belongs to IPT 3. Copy it to a"
            + " new location with the IPT4 migration, then start IPT4 on the new directory:" + System.lineSeparator()
            + "  java -jar ipt.jar migrate --from " + dir + " --to <new data directory>");
        default -> throw new DataDirStartupException("The directory " + dir + " is not empty and not an IPT data"
            + " directory. Point IPT_DATA_DIR to an IPT4 data directory or to an empty directory.");
      }
    } catch (IOException e) {
      throw new DataDirStartupException("Cannot read the data directory " + dir + ": " + e.getMessage(), e);
    }
  }

  private static void clearTmp(DataDir dd) {
    File tmp = dd.tmpFile("");
    try {
      org.apache.commons.io.FileUtils.forceMkdir(tmp);
      org.apache.commons.io.FileUtils.cleanDirectory(tmp);
    } catch (IOException e) {
      LOG.warn("Couldn't clear temporary data dir folder", e);
    }
  }
}
