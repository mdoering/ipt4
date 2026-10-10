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
package org.gbif.ipt.service.manage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLocksTest {

  private final ResourceLocks locks = new ResourceLocks();

  @Test
  void returnsWhatTheActionReturns() {
    assertThat(locks.withWrite("res1", () -> 42)).isEqualTo(42);
    assertThat(locks.withRead("res1", () -> "x")).isEqualTo("x");
  }

  @Test
  void aWriterExcludesReadersOfTheSameResource() throws Exception {
    CountDownLatch writing = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    Thread writer = new Thread(() -> locks.withWrite("Res1", () -> {
      writing.countDown();
      await(finish);
      return null;
    }));
    writer.start();
    assertThat(writing.await(5, TimeUnit.SECONDS)).isTrue();

    AtomicBoolean read = new AtomicBoolean();
    Thread reader = new Thread(() -> locks.withRead("res1", () -> {
      read.set(true);
      return null;
    }));
    reader.start();
    Thread.sleep(200);
    assertThat(read).as("shortnames are case-insensitive, so the reader waits").isFalse();

    finish.countDown();
    reader.join(5000);
    assertThat(read).isTrue();
  }

  @Test
  void differentResourcesDoNotBlockEachOther() throws Exception {
    CountDownLatch writing = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    Thread writer = new Thread(() -> locks.withWrite("res1", () -> {
      writing.countDown();
      await(finish);
      return null;
    }));
    writer.start();
    assertThat(writing.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(locks.withWrite("res2", () -> "free")).isEqualTo("free");
    finish.countDown();
    writer.join(5000);
  }

  @Test
  void readersShareTheLock() throws Exception {
    CountDownLatch reading = new CountDownLatch(1);
    CountDownLatch finish = new CountDownLatch(1);
    Thread reader = new Thread(() -> locks.withRead("res1", () -> {
      reading.countDown();
      await(finish);
      return null;
    }));
    reader.start();
    assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(locks.withRead("res1", () -> "shared")).isEqualTo("shared");
    finish.countDown();
    reader.join(5000);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
