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
import org.gbif.ipt.service.manage.LegacyResourceConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/** Converts resource folders exported from IPT 3 so they can be imported (see {@link LegacyResourceConverter}). */
public class LegacyResourceFolderConverter implements LegacyResourceConverter {

  private final JsonResourcePersister persister;
  private final LegacyXmlReader reader;

  public LegacyResourceFolderConverter(SecretCipher cipher, JdbcSupport jdbc) {
    this.persister = new JsonResourcePersister(cipher, jdbc, new KeysOnly());
    this.reader = new LegacyXmlReader(jdbc);
  }

  @Override
  public boolean convert(File resourceDir) throws IOException {
    Path dir = resourceDir.toPath();
    Path xml = dir.resolve("resource.xml");
    if (!Files.isRegularFile(xml) || Files.exists(dir.resolve(DataDir.PERSISTENCE_FILENAME))) {
      return false;
    }
    Resource resource;
    try (InputStream in = Files.newInputStream(xml)) {
      resource = reader.readResource(in);
    }
    StringWriter json = new StringWriter();
    persister.save(resource, json);
    AtomicFiles.writeString(dir.resolve(DataDir.PERSISTENCE_FILENAME), json.toString());
    Files.delete(xml);

    Path inferredXml = dir.resolve("inferredMetadata.xml");
    if (Files.isRegularFile(inferredXml)) {
      try (InputStream in = Files.newInputStream(inferredXml)) {
        InferredMetadata inferred = reader.readInferredMetadata(in);
        StringWriter w = new StringWriter();
        persister.saveInferredMetadata(inferred, w);
        AtomicFiles.writeString(dir.resolve(DataDir.INFERRED_METADATA_FILENAME), w.toString());
      } catch (IOException e) {
        // inferred metadata is computed again
      }
      Files.delete(inferredXml);
    }
    return true;
  }
}
