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

import org.gbif.ipt.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import tools.jackson.databind.json.JsonMapper;

/** The user accounts in {@code config/users.json}. Passwords are bcrypt hashes and stored as they are. */
public class UserStore {

  public static final String FILENAME = "users.json";

  /** The file's content. */
  public static class UsersFile {
    int schemaVersion = DataDirJson.SCHEMA_VERSION;
    List<User> users = new ArrayList<>();
  }

  private final Path file;
  private final JsonMapper mapper;

  public UserStore(Path file) {
    this.file = file;
    // users hold no encrypted secrets
    this.mapper = DataDirJson.configMapper(new SecretCipher(new byte[32]));
  }

  /** The stored users, or an empty list if there is no file yet. */
  public List<User> load() throws IOException {
    if (!Files.exists(file)) {
      return new ArrayList<>();
    }
    UsersFile content = mapper.readValue(file.toFile(), UsersFile.class);
    return content.users == null ? new ArrayList<>() : content.users;
  }

  public void save(List<User> users) throws IOException {
    UsersFile content = new UsersFile();
    content.users = new ArrayList<>(users);
    content.users.sort(Comparator.comparing(User::getEmail, Comparator.nullsLast(Comparator.naturalOrder())));
    AtomicFiles.write(file, out -> mapper.writeValue(out, content));
  }
}
