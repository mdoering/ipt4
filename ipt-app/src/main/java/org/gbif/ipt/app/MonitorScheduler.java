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
package org.gbif.ipt.app;

import org.gbif.ipt.config.ExtensionMonitor;
import org.gbif.ipt.config.PublishingMonitor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the publishing and extension monitors on Spring's scheduler (IPT 3 started a thread for each). Switched off
 * with {@code ipt.scheduling.enabled=false}, e.g. in tests.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "ipt.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MonitorScheduler {

  private final PublishingMonitor publishingMonitor;
  private final ExtensionMonitor extensionMonitor;

  public MonitorScheduler(PublishingMonitor publishingMonitor, ExtensionMonitor extensionMonitor) {
    this.publishingMonitor = publishingMonitor;
    this.extensionMonitor = extensionMonitor;
  }

  @Scheduled(fixedDelay = PublishingMonitor.MONITOR_INTERVAL_MS, initialDelay = PublishingMonitor.MONITOR_INTERVAL_MS)
  void publishing() {
    publishingMonitor.runScheduled();
  }

  /** Like IPT 3, checks once soon after start-up and then daily. */
  @Scheduled(fixedDelay = ExtensionMonitor.MONITOR_INTERVAL_MS, initialDelay = 60_000)
  void extensions() {
    extensionMonitor.runScheduled();
  }
}
