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

import org.gbif.ipt.model.Registration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

import tools.jackson.databind.json.JsonMapper;

/**
 * The IPT's registration in {@code config/registration.json}: hosting organisation, IPT instance, network and the
 * associated organisations. Organisation and IPT passwords are encrypted with the {@link SecretCipher}.
 */
public class RegistrationStore {

  public static final String FILENAME = "registration.json";

  /** The file's content. */
  public static class RegistrationFile {
    int schemaVersion = DataDirJson.SCHEMA_VERSION;
    Registration registration;
  }

  private final Path file;
  private final JsonMapper mapper;

  public RegistrationStore(Path file, SecretCipher cipher) {
    this.file = file;
    this.mapper = DataDirJson.configMapper(cipher);
  }

  /** The stored registration, or null if the IPT has none yet. */
  @Nullable
  public Registration load() throws IOException {
    if (!Files.exists(file)) {
      return null;
    }
    return mapper.readValue(file.toFile(), RegistrationFile.class).registration;
  }

  public void save(Registration registration) throws IOException {
    RegistrationFile content = new RegistrationFile();
    content.registration = registration;
    AtomicFiles.write(file, out -> mapper.writeValue(out, content));
  }
}
