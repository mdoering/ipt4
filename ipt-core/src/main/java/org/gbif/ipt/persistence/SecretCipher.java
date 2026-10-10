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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts the secrets the IPT has to keep in clear form for later use (SQL source passwords, registry passwords)
 * with AES-256-GCM. Stored values look like {@code {aesgcm}base64(iv | ciphertext | tag)}.
 * <p>
 * The key is the 32-byte file {@value #KEY_FILE} in the config directory, created on first use, or the base64 value
 * of the environment variable {@value #ENV_KEY}.
 */
public final class SecretCipher {

  public static final String KEY_FILE = ".secret.key";
  public static final String ENV_KEY = "IPT_SECRET_KEY";
  public static final String PREFIX = "{aesgcm}";

  private static final int KEY_BYTES = 32;
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String KEY_CHECK = "ipt-secret-key-check";

  /** A secret could not be read: wrong key, corrupt value or unusable key. */
  public static class SecretCipherException extends RuntimeException {
    public SecretCipherException(String message, Throwable cause) {
      super(message, cause);
    }

    public SecretCipherException(String message) {
      super(message);
    }
  }

  private final SecretKeySpec key;

  public SecretCipher(byte[] key) {
    if (key.length != KEY_BYTES) {
      throw new SecretCipherException("The IPT secret key must be " + KEY_BYTES + " bytes, but has " + key.length);
    }
    this.key = new SecretKeySpec(key.clone(), "AES");
  }

  /**
   * Makes the key of a new installation: the base64 value of {@value #ENV_KEY} if set, otherwise 32 random bytes
   * written to {@value #KEY_FILE} (owner-only). An existing key file is never replaced.
   */
  public static SecretCipher create(Path configDir, Map<String, String> env) throws IOException {
    SecretCipher fromEnv = fromEnv(env);
    if (fromEnv != null) {
      return fromEnv;
    }
    Path keyFile = configDir.resolve(KEY_FILE);
    if (Files.exists(keyFile)) {
      throw new SecretCipherException("Refusing to replace the existing secret key " + keyFile);
    }
    byte[] fresh = new byte[KEY_BYTES];
    RANDOM.nextBytes(fresh);
    AtomicFiles.write(keyFile, out -> out.write(fresh));
    try {
      Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
    } catch (UnsupportedOperationException e) {
      // not a POSIX file system
    }
    return new SecretCipher(fresh);
  }

  /** The key of an existing installation: {@value #ENV_KEY} if set, otherwise {@value #KEY_FILE}; never made up. */
  public static SecretCipher load(Path configDir, Map<String, String> env) throws IOException {
    SecretCipher fromEnv = fromEnv(env);
    if (fromEnv != null) {
      return fromEnv;
    }
    Path keyFile = configDir.resolve(KEY_FILE);
    if (!Files.exists(keyFile)) {
      throw new SecretCipherException("The secret key is missing: restore config/" + KEY_FILE
          + " from the backup of this data directory, or set " + ENV_KEY + ". Without it the stored passwords cannot"
          + " be read.");
    }
    return new SecretCipher(Files.readAllBytes(keyFile));
  }

  @Nullable
  private static SecretCipher fromEnv(Map<String, String> env) {
    String fromEnv = env.get(ENV_KEY);
    if (fromEnv == null || fromEnv.isBlank()) {
      return null;
    }
    try {
      return new SecretCipher(Base64.getDecoder().decode(fromEnv.trim()));
    } catch (IllegalArgumentException e) {
      throw new SecretCipherException(ENV_KEY + " is not valid base64", e);
    }
  }

  /** A value only this key can read back, stored in the data dir marker to recognise the key. */
  public String keyCheck() {
    return encrypt(KEY_CHECK);
  }

  /** Whether this is the key that wrote {@code keyCheck}. */
  public boolean verifies(@Nullable String keyCheck) {
    try {
      return KEY_CHECK.equals(decrypt(keyCheck));
    } catch (SecretCipherException e) {
      return false;
    }
  }

  public static boolean isEncrypted(@Nullable String value) {
    return value != null && value.startsWith(PREFIX);
  }

  @Nullable
  public String encrypt(@Nullable String plain) {
    if (plain == null) {
      return null;
    }
    try {
      byte[] iv = new byte[IV_BYTES];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
    } catch (GeneralSecurityException e) {
      throw new SecretCipherException("Cannot encrypt secret", e);
    }
  }

  /** Decrypts a value written by {@link #encrypt}. Values without the prefix are returned unchanged. */
  @Nullable
  public String decrypt(@Nullable String stored) {
    if (stored == null || !isEncrypted(stored)) {
      return stored;
    }
    try {
      byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
      return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new SecretCipherException("Cannot decrypt a stored secret. Was config/" + KEY_FILE
          + " replaced, or " + ENV_KEY + " changed?", e);
    }
  }
}
