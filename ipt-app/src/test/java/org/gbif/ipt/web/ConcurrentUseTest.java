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
package org.gbif.ipt.web;

import org.gbif.ipt.app.ManageFixture;
import org.gbif.ipt.app.PortalFixture;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Portal and manage reads, metadata and resource saves and a publication at the same time: no server errors, no
 * deadlock (M4's lock-order review finding), reasonable response times.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentUseTest {

  private static final String ADMIN = "concurrency.admin@example.org";
  private static final List<String> PAGES = List.of("/", "/resource.do?r=res1", "/rss.do", "/eml.do?r=res1",
      "/api/resources?start=0&length=10", "/manage/", "/manage/resource.do?r=res1",
      "/manage/metadata-basic.do?r=res1", "/manage/publication-settings.do?r=res1", "/admin/");

  static Path dataDir;

  @DynamicPropertySource
  static void dataDir(DynamicPropertyRegistry registry) throws Exception {
    dataDir = ManageFixture.create();
    registry.add("ipt.data-dir", dataDir::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired UserAccountManager users;
  @Autowired ResourceManager resources;
  @Autowired ResourcePublicationManager publications;
  @Autowired RegistrationManager registrations;

  @Test
  void readsSavesAndAPublicationAtOnce() throws Exception {
    ManageFixture.user(users, ADMIN, User.Role.Admin);
    PortalFixture.publishRes1(dataDir, resources, publications, users, registrations);
    List<String> problems = Collections.synchronizedList(new ArrayList<>());
    List<Long> durations = Collections.synchronizedList(new ArrayList<>());
    ExecutorService threads = Executors.newFixedThreadPool(6);
    List<Future<?>> work = new ArrayList<>();
    for (int t = 0; t < 4; t++) {
      work.add(threads.submit(() -> {
        for (int i = 0; i < 50; i++) {
          String page = PAGES.get(i % PAGES.size());
          long start = System.nanoTime();
          int status = mvc.perform(get(URI.create(page)).with(user(ADMIN))).andReturn().getResponse().getStatus();
          durations.add((System.nanoTime() - start) / 1_000_000);
          if (status >= 500) {
            problems.add(status + " " + page);
          }
        }
        return null;
      }));
    }
    work.add(threads.submit(() -> {
      for (int i = 0; i < 20; i++) {
        Resource res1 = resources.get("res1");
        resources.getLocks().withWrite("res1", () -> {
          resources.saveEml(res1);
          return null;
        });
        resources.save(res1);
      }
      return null;
    }));
    work.add(threads.submit(() -> {
      Resource res1 = resources.get("res1");
      publications.publish(res1, res1.getNextVersion(), new org.gbif.ipt.feedback.CollectingFeedback(null, users.get(ADMIN)));
      while (publications.isLocked("res1")) {
        Thread.sleep(50);
      }
      return null;
    }));
    try {
      for (Future<?> f : work) {
        // a deadlock shows as a timeout
        f.get(120, TimeUnit.SECONDS);
      }
    } finally {
      threads.shutdownNow();
    }
    assertThat(problems).isEmpty();
    List<Long> sorted = new ArrayList<>(durations);
    Collections.sort(sorted);
    long p95 = sorted.get((int) (sorted.size() * 0.95) - 1);
    assertThat(p95).as("95th percentile response time (ms)").isLessThan(5_000);
  }
}
