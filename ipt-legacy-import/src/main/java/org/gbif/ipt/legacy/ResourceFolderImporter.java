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

import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.config.JdbcSupport;
import org.gbif.ipt.model.InferredMetadata;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.persistence.AtomicFiles;
import org.gbif.ipt.persistence.JsonResourcePersister;
import org.gbif.ipt.persistence.SecretCipher;
import org.gbif.ipt.service.manage.ResourceFolderImport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Prepares uploaded resource folders (see {@link ResourceFolderImport}): IPT 3's {@code resource.xml} and
 * {@code inferredMetadata.xml} become JSON; SQL source passwords encrypted by another installation are dropped.
 */
public class ResourceFolderImporter implements ResourceFolderImport {

  private static final JsonMapper TREE = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private final SecretCipher cipher;
  private final JsonResourcePersister persister;
  private final JdbcSupport jdbc;

  public ResourceFolderImporter(SecretCipher cipher, JdbcSupport jdbc) {
    this.cipher = cipher;
    this.jdbc = jdbc;
    this.persister = new JsonResourcePersister(cipher, jdbc, new KeysOnly());
  }

  @Override
  public List<String> prepare(File resourceDir) throws IOException {
    Path dir = resourceDir.toPath();
    List<String> dropped = convertIpt3(dir);
    Path json = dir.resolve(DataDir.PERSISTENCE_FILENAME);
    if (Files.isRegularFile(json)) {
      dropped.addAll(dropForeignSecrets(json));
    }
    return dropped;
  }

  /** @return sources whose IPT 3 password could not be decrypted */
  private List<String> convertIpt3(Path dir) throws IOException {
    List<String> dropped = new ArrayList<>();
    Path xml = dir.resolve("resource.xml");
    if (!Files.isRegularFile(xml) || Files.exists(dir.resolve(DataDir.PERSISTENCE_FILENAME))) {
      return dropped;
    }
    // one reader per folder, so its problems belong to this upload
    LegacyXmlReader reader = new LegacyXmlReader(jdbc);
    Resource resource;
    try (InputStream in = Files.newInputStream(xml)) {
      resource = reader.readResource(in);
    }
    StringWriter w = new StringWriter();
    persister.save(resource, w);
    AtomicFiles.writeString(dir.resolve(DataDir.PERSISTENCE_FILENAME), w.toString());
    Files.delete(xml);
    dropped.addAll(reader.takeDroppedSecrets());

    Path inferredXml = dir.resolve("inferredMetadata.xml");
    if (Files.isRegularFile(inferredXml)) {
      try (InputStream in = Files.newInputStream(inferredXml)) {
        InferredMetadata inferred = reader.readInferredMetadata(in);
        StringWriter iw = new StringWriter();
        persister.saveInferredMetadata(inferred, iw);
        AtomicFiles.writeString(dir.resolve(DataDir.INFERRED_METADATA_FILENAME), iw.toString());
      } catch (IOException e) {
        // inferred metadata is computed again
      }
      Files.delete(inferredXml);
    }
    return dropped;
  }

  private List<String> dropForeignSecrets(Path json) throws IOException {
    List<String> dropped = new ArrayList<>();
    JsonNode root = TREE.readTree(json.toFile());
    JsonNode sources = root.get("sources");
    if (sources == null || !sources.isArray()) {
      return dropped;
    }
    for (JsonNode source : sources) {
      JsonNode password = source.get("password");
      if (source instanceof ObjectNode o && password != null && password.isString()
          && SecretCipher.isEncrypted(password.asString())) {
        try {
          cipher.decrypt(password.asString());
        } catch (SecretCipher.SecretCipherException e) {
          o.remove("password");
          dropped.add(source.path("name").asString());
        }
      }
    }
    if (!dropped.isEmpty()) {
      AtomicFiles.writeString(json, TREE.writeValueAsString(root));
    }
    return dropped;
  }
}
