package org.gbif.ipt.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
}
