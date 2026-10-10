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

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs long jobs (publications, later imports) in the background, at most {@code maxThreads} at a time, one per key.
 * Closing the service cancels running jobs, so the IPT can shut down without leaving threads behind.
 */
public class JobService implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(JobService.class);
  private static final long SHUTDOWN_WAIT_SECONDS = 30;

  private final ThreadPoolExecutor executor;
  private final Map<String, JobHandle<?>> jobs = new ConcurrentHashMap<>();

  public JobService(int maxThreads) {
    int threads = Math.max(1, maxThreads);
    AtomicInteger count = new AtomicInteger();
    ThreadFactory factory = r -> {
      Thread t = new Thread(r, "ipt-job-" + count.incrementAndGet());
      t.setDaemon(true);
      return t;
    };
    this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        factory);
  }

  /**
   * Starts a job for {@code key}.
   *
   * @throws IllegalStateException if a job for the key is still running, or the service is closed
   */
  public synchronized <T> JobHandle<T> submit(String key, String type, Callable<T> task) {
    if (executor.isShutdown()) {
      throw new IllegalStateException("The job service is shut down");
    }
    JobHandle<?> existing = jobs.get(key);
    if (existing != null && !existing.isDone()) {
      throw new IllegalStateException("A " + existing.type() + " job for " + key + " is still running");
    }
    Future<T> future = executor.submit(task);
    JobHandle<T> handle = new JobHandle<>(key, type, Instant.now(), future);
    jobs.put(key, handle);
    return handle;
  }

  public Optional<JobHandle<?>> get(String key) {
    return Optional.ofNullable(jobs.get(key));
  }

  public Collection<JobHandle<?>> jobs() {
    return List.copyOf(jobs.values());
  }

  /** Forgets a job, e.g. once its result has been processed. */
  public void remove(String key) {
    jobs.remove(key);
  }

  /** Cancels the job for {@code key}, interrupting it if it runs. */
  public boolean cancel(String key) {
    JobHandle<?> handle = jobs.get(key);
    return handle != null && handle.future().cancel(true);
  }

  public int availableSlots() {
    return executor.getMaximumPoolSize() - executor.getActiveCount();
  }

  /** The underlying executor, for IPT 3 code that inspects it. */
  public ThreadPoolExecutor executor() {
    return executor;
  }

  @Override
  public void close() {
    jobs.values().stream().filter(j -> !j.isDone()).forEach(j -> {
      LOG.warn("Cancelling {} job for {} on shutdown", j.type(), j.key());
      j.future().cancel(true);
    });
    executor.shutdownNow();
    try {
      if (!executor.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
        LOG.warn("Background jobs did not stop within {} seconds", SHUTDOWN_WAIT_SECONDS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
