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

import org.gbif.ipt.legacy.v3.PBEEncrypt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Builds IPT 3 data dirs from the legacy test fixtures. */
final class Ipt3Fixtures {

  static final String ADMIN_HASH = "$2a$12$abcdefghijklmnopqrstuuWb0V4o6M5wF3bC7m7FvKX9i3uXlG3l2";

  private Ipt3Fixtures() {
  }

  static void copy(String resource, Path target) throws IOException {
    Files.createDirectories(target.getParent());
    try (InputStream in = Ipt3Fixtures.class.getResourceAsStream("/ipt3/" + resource)) {
      if (in == null) {
        throw new IOException("Missing fixture " + resource);
      }
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  static String pbe(String plain) throws PBEEncrypt.EncryptionException {
    return LegacyXmlReader.legacyPbe().encrypt(plain);
  }

  /** An IPT 3 data dir with users, an encrypted registration and three resources. */
  static Path dataDir(Path dir) throws Exception {
    copy("config/ipt.properties", dir.resolve("config/ipt.properties"));
    copy("config/registration2.xml", dir.resolve("config/registration2.xml"));
    Files.writeString(dir.resolve("config/.gbifreg"), "DEVELOPMENT");
    Files.createDirectories(dir.resolve("config/.extensions"));
    Files.writeString(dir.resolve("config/.extensions/occurrence.xml"), "<extension/>");
    Files.writeString(dir.resolve("config/users.xml"), """
        <users>
          <user email="admin@gbif.org" password="%s" firstname="Ada" lastname="Admin" role="Admin" lastLogin="2020-01-02 10:11:12.0 UTC"/>
          <user email="old@gbif.org" password="%s" firstname="Old" lastname="Timer" role="Manager"/>
        </users>
        """.formatted(ADMIN_HASH, pbe("secret")));
    for (String r : new String[] {"res1", "amphibians", "event"}) {
      copy("resources/" + r + "/resource.xml", dir.resolve("resources/" + r + "/resource.xml"));
      copy("resources/" + r + "/eml.xml", dir.resolve("resources/" + r + "/eml.xml"));
    }
    copy("resources/res1/occurrence.txt", dir.resolve("resources/res1/sources/occurrence.txt"));
    Files.writeString(dir.resolve("resources/res1/eml-1.1.xml"), "<eml/>");
    Files.createDirectories(dir.resolve("logs"));
    Files.writeString(dir.resolve("logs/debug.log"), "log");
    return dir;
  }
}
