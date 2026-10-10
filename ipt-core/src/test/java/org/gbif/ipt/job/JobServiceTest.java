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
package org.gbif.ipt.job;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobServiceTest {

  private final JobService jobs = new JobService(2);

  @AfterEach
  void close() {
    jobs.close();
  }

  @Test
  void runsJobsAndKeepsTheirHandles() throws Exception {
    JobHandle<String> handle = jobs.submit("res1", "publication", () -> "done");
    assertThat(handle.key()).isEqualTo("res1");
    assertThat(handle.type()).isEqualTo("publication");
    assertThat(handle.future().get(5, TimeUnit.SECONDS)).isEqualTo("done");
    assertThat(jobs.get("res1")).containsSame(handle);
    jobs.remove("res1");
    assertThat(jobs.get("res1")).isEmpty();
  }

  @Test
  void runsAtMostMaxThreadsJobsAtOnce() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger running = new AtomicInteger();
    AtomicInteger maxRunning = new AtomicInteger();
    for (int i = 0; i < 4; i++) {
      jobs.submit("r" + i, "publication", () -> {
        maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
        release.await(5, TimeUnit.SECONDS);
        running.decrementAndGet();
        return null;
      });
    }
    Thread.sleep(300);
    assertThat(maxRunning.get()).isEqualTo(2);
    assertThat(jobs.availableSlots()).isZero();
    release.countDown();
    for (int i = 0; i < 4; i++) {
      jobs.get("r" + i).orElseThrow().future().get(5, TimeUnit.SECONDS);
    }
    assertThat(maxRunning.get()).isEqualTo(2);
  }

  @Test
  void refusesASecondJobForAKeyThatIsStillRunning() {
    CountDownLatch release = new CountDownLatch(1);
    jobs.submit("res1", "publication", () -> release.await(5, TimeUnit.SECONDS));
    assertThatThrownBy(() -> jobs.submit("res1", "publication", () -> null))
        .isInstanceOf(IllegalStateException.class);
    release.countDown();
  }

  @Test
  void cancelInterruptsARunningJob() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    AtomicBoolean interrupted = new AtomicBoolean();
    JobHandle<Object> handle = jobs.submit("res1", "publication", () -> {
      started.countDown();
      try {
        Thread.sleep(10_000);
      } catch (InterruptedException e) {
        interrupted.set(true);
      }
      return null;
    });
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(jobs.cancel("res1")).isTrue();
    assertThat(handle.future().isCancelled()).isTrue();
    Thread.sleep(200);
    assertThat(interrupted).isTrue();
    assertThat(jobs.cancel("unknown")).isFalse();
  }

  @Test
  void closeCancelsRunningJobsAndStopsTheExecutor() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    JobHandle<Object> handle = jobs.submit("res1", "publication", () -> {
      started.countDown();
      Thread.sleep(10_000);
      return null;
    });
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
    jobs.close();
    assertThat(handle.future().isDone()).isTrue();
    assertThat(jobs.executor().isTerminated()).isTrue();
    assertThatThrownBy(() -> jobs.submit("res2", "publication", () -> null))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void failedJobsReportTheirException() {
    JobHandle<Object> handle = jobs.submit("res1", "publication", () -> {
      throw new IllegalArgumentException("boom");
    });
    assertThatThrownBy(() -> handle.future().get(5, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class).hasRootCauseMessage("boom");
  }
}
