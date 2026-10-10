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

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Test data dirs count as set up (an admin and a registry type), so pages are not sent to the setup wizard. Tests of
 * the wizard itself set {@code test.ipt.complete-setup=false}.
 */
@Component
@ConditionalOnProperty(name = "test.ipt.complete-setup", havingValue = "true")
public class CompleteSetupForTests implements SmartInitializingSingleton {

  public static final String ADMIN = "setup.admin@example.org";

  private final AppConfig cfg;
  private final UserAccountManager users;

  public CompleteSetupForTests(AppConfig cfg, UserAccountManager users) {
    this.cfg = cfg;
    this.users = users;
  }

  @Override
  public void afterSingletonsInstantiated() {
    try {
      if (cfg.getRegistryType() == null) {
        cfg.setRegistryType(AppConfig.REGISTRY_TYPE.DEVELOPMENT);
      }
      if (users.list(User.Role.Admin).isEmpty()) {
        User admin = new User();
        admin.setEmail(ADMIN);
        admin.setFirstname("Setup");
        admin.setLastname("Admin");
        admin.setRole(User.Role.Admin);
        admin.setPassword("Secret-pass-1");
        users.create(admin);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot complete the setup of the test data dir", e);
    }
  }
}
