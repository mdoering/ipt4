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

import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * The portal fixture with the schemas, core extensions and vocabularies an IPT installs from the GBIF registry
 * (captured in {@code installed-config}), for the manage pages.
 */
public final class ManageFixture {

  /** A manager of res1. */
  public static final String OWNER = "owner@example.org";
  /** A manager of nothing. */
  public static final String OTHER = "other@example.org";
  public static final String PASSWORD = "Secret-pass-1";

  private ManageFixture() {
  }

  public static Path create() throws Exception {
    Path dir = PortalFixture.create();
    File config = dir.resolve("config").toFile();
    // the IPT 3 fixture's older copies would load next to the installed ones
    for (String installed : List.of(".extensions", ".vocabularies")) {
      FileUtils.deleteDirectory(new File(config, installed));
    }
    FileUtils.copyDirectory(new File(ManageFixture.class.getResource("/installed-config").toURI()), config);
    return dir;
  }

  public static User user(UserAccountManager users, String email, User.Role role) throws Exception {
    User user = users.get(email);
    if (user == null) {
      user = new User();
      user.setEmail(email);
      user.setFirstname(role.name());
      user.setLastname("Tester");
      user.setRole(role);
      user.setPassword(PASSWORD);
      users.create(user);
    }
    return users.get(email);
  }
}
