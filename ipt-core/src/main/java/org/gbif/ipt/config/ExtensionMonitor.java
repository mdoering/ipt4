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

import org.gbif.ipt.model.Extension;
import org.gbif.ipt.service.admin.ExtensionManager;
import org.gbif.ipt.service.registry.RegistryManager;

import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Makes sure IPT is aware of any recent updates to the extensions.
 */
public class ExtensionMonitor {

  // 24h interval
  public static final int MONITOR_INTERVAL_MS = 86_400_000;
  private static final Logger LOG = LogManager.getLogger(ExtensionMonitor.class);
  private final AtomicBoolean running = new AtomicBoolean();
  private final ExtensionManager extensionManager;
  private final RegistryManager registryManager;

  @Inject
  public ExtensionMonitor(ExtensionManager extensionManager, RegistryManager registryManager) {
    this.extensionManager = extensionManager;
    this.registryManager = registryManager;
  }

  /**
   * Called every {@link #MONITOR_INTERVAL_MS} ms by the application's scheduler (IPT 3 ran its own thread): marks
   * installed extensions that have a newer version in the registry. Does nothing until {@link #start()} was called.
   */
  public void runScheduled() {
    if (!running.get()) {
      return;
    }
    try {
      List<Extension> installedExtensions = extensionManager.list();
      List<Extension> registeredExtensions = registryManager.getLatestExtensions();

      for (Extension extension : installedExtensions) {
        for (Extension rExtension : registeredExtensions) {
          // check if registered extension is latest, and if it is, try to use it in comparison
          if (extension.getRowType().equalsIgnoreCase(rExtension.getRowType())) {
            Date installedExtensionIssuedDate = extension.getIssued();
            Date latestExtensionIssuedDate = rExtension.getIssued();
            if (installedExtensionIssuedDate == null && latestExtensionIssuedDate != null) {
              extension.setLatest(false);
            } else if (latestExtensionIssuedDate != null && latestExtensionIssuedDate.compareTo(installedExtensionIssuedDate) > 0) {
              extension.setLatest(false);
            }
            break;
          }
        }
      }
    } catch (RuntimeException e) {
      LOG.error("Cannot check the installed extensions against the registry", e);
    }
  }

  /**
   * Starts the monitor: from now on {@link #runScheduled()} does its work.
   */
  public void start() {
    running.set(true);
    LOG.debug("The extension monitor has started.");
  }

  /**
   * Stops the monitor.
   */
  public void stop() {
    running.set(false);
  }
}
