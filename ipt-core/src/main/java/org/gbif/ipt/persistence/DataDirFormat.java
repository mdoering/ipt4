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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import tools.jackson.databind.json.JsonMapper;

/**
 * Tells IPT4 data dirs from IPT 3 data dirs. An IPT4 data dir has the marker file {@value #MARKER}; it is written
 * last when a data dir is created or migrated, so a half-migrated directory is never taken for a valid one.
 */
public final class DataDirFormat {

  public static final String MARKER = "ipt-datadir.json";
  public static final int CURRENT_VERSION = 1;

  public enum Kind {
    /** Missing, or holding nothing but hidden files: a new IPT is set up here. */
    EMPTY,
    /** An IPT4 data dir. */
    V4,
    /** An IPT 3 data dir, which must be migrated with {@code ipt migrate} first. */
    IPT3,
    /** A migration into this directory did not finish (it has a secret key but no marker). */
    INCOMPLETE,
    /** Something else; the IPT refuses to touch it. */
    UNKNOWN
  }

  /** The marker's content. */
  public record Marker(String format, int version, String createdBy, String created,
                       @Nullable Map<String, Object> migratedFrom) {
  }

  private static final JsonMapper MAPPER = JsonMapper.builder()
      .enable(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT)
      .changeDefaultPropertyInclusion(i -> i.withValueInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL))
      .build();

  private DataDirFormat() {
  }

  public static Kind detect(Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return Kind.EMPTY;
    }
    if (Files.isRegularFile(dir.resolve(MARKER))) {
      return Kind.V4;
    }
    try (Stream<Path> entries = Files.list(dir)) {
      if (entries.allMatch(p -> p.getFileName().toString().startsWith("."))) {
        return Kind.EMPTY;
      }
    }
    // migrations create the key first and the marker last; IPT 3 never had a key
    if (Files.isRegularFile(dir.resolve("config").resolve(SecretCipher.KEY_FILE))) {
      return Kind.INCOMPLETE;
    }
    if (Files.isRegularFile(dir.resolve("config/ipt.properties")) || Files.isRegularFile(dir.resolve("config/users.xml"))) {
      return Kind.IPT3;
    }
    return Kind.UNKNOWN;
  }

  public static void writeMarker(Path dir, String createdBy, @Nullable Map<String, Object> migratedFrom)
      throws IOException {
    Marker marker = new Marker("ipt", CURRENT_VERSION, createdBy, Instant.now().toString(), migratedFrom);
    AtomicFiles.write(dir.resolve(MARKER), out -> MAPPER.writeValue(out, marker));
  }

  public static Marker readMarker(Path dir) {
    return MAPPER.readValue(dir.resolve(MARKER).toFile(), Marker.class);
  }
}
