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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonitorSchedulerTest {

  @Test
  void drivesBothMonitors() {
    PublishingMonitor publishing = mock(PublishingMonitor.class);
    ExtensionMonitor extensions = mock(ExtensionMonitor.class);
    MonitorScheduler scheduler = new MonitorScheduler(publishing, extensions);
    scheduler.publishing();
    scheduler.extensions();
    verify(publishing).runScheduled();
    verify(extensions).runScheduled();
  }

  @Test
  void keepsTheIpt3Intervals() throws Exception {
    Method publishing = MonitorScheduler.class.getDeclaredMethod("publishing");
    Method extensions = MonitorScheduler.class.getDeclaredMethod("extensions");
    assertThat(publishing.getAnnotation(Scheduled.class).fixedDelay()).isEqualTo(PublishingMonitor.MONITOR_INTERVAL_MS);
    assertThat(extensions.getAnnotation(Scheduled.class).fixedDelay()).isEqualTo(ExtensionMonitor.MONITOR_INTERVAL_MS);
  }
}
