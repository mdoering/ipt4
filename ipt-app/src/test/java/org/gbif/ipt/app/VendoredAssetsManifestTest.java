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

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Every vendored library listed in tools/assets/manifest.json exists, in the one-line form Renovate parses. */
class VendoredAssetsManifestTest {

  private static final Path MANIFEST = Path.of("../tools/assets/manifest.json");
  private static final Path STATIC = Path.of("src/main/resources/static");

  @Test
  void listedFilesExistAndEntriesAreRenovateReadable() throws Exception {
    String text = Files.readString(MANIFEST);
    JsonNode libraries = JsonMapper.builder().build().readTree(text).get("libraries");

    assertThat(libraries.size()).isGreaterThan(10);
    for (int i = 0; i < libraries.size(); i++) {
      JsonNode library = libraries.get(i);
      JsonNode paths = library.get("paths");
      for (int j = 0; j < paths.size(); j++) {
        assertThat(STATIC.resolve(paths.get(j).asString())).as(library.get("name").asString()).exists();
      }
      if (!library.get("npm").isNull() && !library.get("version").isNull()) {
        assertThat(text)
            .contains(
                "\"npm\": \"" + library.get("npm").asString() + "\", \"version\": \""
                    + library.get("version").asString() + "\"");
      }
    }
  }

  /** The vendored Bootstrap files are the version the manifest names (their banner says so). */
  @Test
  void bootstrapFilesAreTheManifestVersion() throws Exception {
    JsonNode libraries = JsonMapper.builder().build().readTree(Files.readString(MANIFEST)).get("libraries");
    for (int i = 0; i < libraries.size(); i++) {
      JsonNode library = libraries.get(i);
      if (library.get("name").asString().equals("bootstrap")) {
        String version = library.get("version").asString();
        for (JsonNode path : library.get("paths")) {
          String head = Files.readString(STATIC.resolve(path.asString())).substring(0, 200);
          assertThat(head).as(path.asString()).containsPattern("Bootstrap\\s+v" + java.util.regex.Pattern.quote(version) + "\\s");
        }
        return;
      }
    }
    throw new AssertionError("no bootstrap entry in the manifest");
  }
}
