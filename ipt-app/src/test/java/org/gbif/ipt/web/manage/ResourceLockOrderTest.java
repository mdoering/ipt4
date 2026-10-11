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
package org.gbif.ipt.web.manage;

import org.gbif.ipt.app.ManageFixture;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.service.manage.ResourceManager;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A metadata save holds the resource's write lock while it persists; another save of the same resource must wait
 * for it, not deadlock with it (the manager's monitor was taken in the opposite order).
 */
@SpringBootTest
class ResourceLockOrderTest {

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired ResourceManager resources;

  @Test
  void saveDuringALockedMetadataSaveDoesNotDeadlock() throws Exception {
    Resource resource = resources.get("res1");
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch otherSaveStarted = new CountDownLatch(1);
    ExecutorService threads = Executors.newFixedThreadPool(2);
    try {
      Future<?> metadataSave = threads.submit(() -> resources.getLocks().withWrite(resource.getShortname(), () -> {
        locked.countDown();
        try {
          otherSaveStarted.await();
          // give the other save time to block on the resource lock
          Thread.sleep(300);
          resources.saveEml(resource);
          resources.saveDatapackageMetadata(resource);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
        return null;
      }));
      Future<?> otherSave = threads.submit(() -> {
        locked.await();
        otherSaveStarted.countDown();
        resources.save(resource);
        return null;
      });
      metadataSave.get(10, TimeUnit.SECONDS);
      otherSave.get(10, TimeUnit.SECONDS);
    } finally {
      threads.shutdownNow();
    }
    assertThat(resources.get("res1")).isNotNull();
  }
}
