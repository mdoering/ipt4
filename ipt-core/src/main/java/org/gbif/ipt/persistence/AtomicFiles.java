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
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.output.CloseShieldOutputStream;

/**
 * Replaces files all at once: content goes to a temporary file next to the target, is flushed to disk and then moved
 * over the target. A failure while writing leaves the old file untouched.
 */
public final class AtomicFiles {

  @FunctionalInterface
  public interface IOConsumer<T> {
    void accept(T t) throws IOException;
  }

  private AtomicFiles() {
  }

  public static void writeString(Path target, String content) throws IOException {
    write(target, out -> out.write(content.getBytes(StandardCharsets.UTF_8)));
  }

  public static void write(Path target, IOConsumer<OutputStream> body) throws IOException {
    Path absolute = target.toAbsolutePath();
    Path dir = absolute.getParent();
    Files.createDirectories(dir);
    Path tmp = Files.createTempFile(dir, "." + absolute.getFileName(), ".tmp");
    try {
      try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
           OutputStream out = Channels.newOutputStream(channel)) {
        // the body may close what it is given (Jackson does); the channel must stay open to be forced
        body.accept(CloseShieldOutputStream.wrap(out));
        out.flush();
        channel.force(true);
      }
      try {
        Files.move(tmp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tmp);
    }
  }
}
