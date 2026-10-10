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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretCipherTest {

  @TempDir
  Path config;

  @Test
  void roundTripsSecrets() throws IOException {
    SecretCipher cipher = SecretCipher.forConfigDir(config, Map.of());
    String stored = cipher.encrypt("s3cr3t ünïcode");
    assertThat(stored).startsWith("{aesgcm}").doesNotContain("s3cr3t");
    assertThat(cipher.decrypt(stored)).isEqualTo("s3cr3t ünïcode");
    assertThat(cipher.encrypt("s3cr3t ünïcode")).as("random IV").isNotEqualTo(stored);
  }

  @Test
  void createsAnOwnerOnlyKeyFileAndReusesIt() throws IOException {
    SecretCipher first = SecretCipher.forConfigDir(config, Map.of());
    Path keyFile = config.resolve(SecretCipher.KEY_FILE);
    assertThat(Files.size(keyFile)).isEqualTo(32);
    assertThat(Files.getPosixFilePermissions(keyFile))
        .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    String stored = first.encrypt("x");
    assertThat(SecretCipher.forConfigDir(config, Map.of()).decrypt(stored)).isEqualTo("x");
  }

  @Test
  void wrongKeyFailsLoudly() throws IOException {
    String stored = SecretCipher.forConfigDir(config, Map.of()).encrypt("x");
    SecretCipher other = new SecretCipher(new byte[32]);
    assertThatThrownBy(() -> other.decrypt(stored))
        .isInstanceOf(SecretCipher.SecretCipherException.class)
        .hasMessageContaining(SecretCipher.KEY_FILE);
  }

  @Test
  void environmentKeyOverridesTheKeyFile() throws IOException {
    byte[] key = new byte[32];
    key[0] = 7;
    Map<String, String> env = Map.of(SecretCipher.ENV_KEY, Base64.getEncoder().encodeToString(key));
    String stored = SecretCipher.forConfigDir(config, env).encrypt("x");
    assertThat(new SecretCipher(key).decrypt(stored)).isEqualTo("x");
    assertThat(config.resolve(SecretCipher.KEY_FILE)).doesNotExist();
  }

  @Test
  void rejectsKeysOfTheWrongLength() {
    Map<String, String> env = Map.of(SecretCipher.ENV_KEY, Base64.getEncoder().encodeToString(new byte[16]));
    assertThatThrownBy(() -> SecretCipher.forConfigDir(config, env))
        .isInstanceOf(SecretCipher.SecretCipherException.class);
  }

  @Test
  void leavesNullAndPlainValuesAlone() {
    SecretCipher cipher = new SecretCipher(new byte[32]);
    assertThat(cipher.encrypt(null)).isNull();
    assertThat(cipher.decrypt(null)).isNull();
    assertThat(SecretCipher.isEncrypted("plain")).isFalse();
    assertThat(SecretCipher.isEncrypted(cipher.encrypt("x"))).isTrue();
  }
}
