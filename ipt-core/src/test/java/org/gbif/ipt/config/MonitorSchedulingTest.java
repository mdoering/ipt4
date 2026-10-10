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
package org.gbif.ipt.config;

import org.gbif.ipt.i18n.SimpleTextProvider;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.service.RegistryException;
import org.gbif.ipt.service.admin.ExtensionManager;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.manage.ResourceManager;
import org.gbif.ipt.service.manage.ResourcePublicationManager;
import org.gbif.ipt.service.registry.RegistryManager;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The monitors run on the application's scheduler, and only once ConfigManager has started them. */
class MonitorSchedulingTest {

  @Test
  void publishingMonitorOnlyWorksWhileStarted() {
    ResourceManager resources = mock(ResourceManager.class);
    ResourcePublicationManager publications = mock(ResourcePublicationManager.class);
    when(publications.getProcessFutures()).thenReturn(new HashMap<>());
    when(publications.getExecutor()).thenReturn(
        new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>()));
    when(resources.list()).thenReturn(List.of());
    PublishingMonitor monitor = new PublishingMonitor(mock(SimpleTextProvider.class), mock(AppConfig.class),
        mock(RegistrationManager.class), resources, publications);

    monitor.runScheduled();
    verify(resources, never()).list();

    monitor.start();
    monitor.runScheduled();
    verify(resources).list();

    monitor.stop();
    monitor.runScheduled();
    verify(resources).list();
  }

  @Test
  void extensionMonitorSurvivesRegistryOutages() {
    ExtensionManager extensions = mock(ExtensionManager.class);
    RegistryManager registry = mock(RegistryManager.class);
    when(extensions.list()).thenReturn(List.of(new Extension()));
    when(registry.getLatestExtensions()).thenThrow(new RegistryException(RegistryException.Type.IO_ERROR, "x", "down"));
    ExtensionMonitor monitor = new ExtensionMonitor(extensions, registry);

    monitor.runScheduled();
    verify(registry, never()).getLatestExtensions();

    monitor.start();
    assertThatCode(monitor::runScheduled).doesNotThrowAnyException();
    verify(registry).getLatestExtensions();
  }
}
