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
package org.gbif.ipt.service.manage;

import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.User;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceAccessPolicyTest {

  private static User user(String email, User.Role role) {
    User u = new User();
    u.setEmail(email);
    u.setRole(role);
    return u;
  }

  private final User admin = user("admin@x.org", User.Role.Admin);
  private final User creator = user("creator@x.org", User.Role.Manager);
  private final User comanager = user("co@x.org", User.Role.Manager);
  private final User other = user("other@x.org", User.Role.Manager);
  private final User publisher = user("pub@x.org", User.Role.Publisher);
  private final User plain = user("user@x.org", User.Role.User);

  private Resource resource() {
    Resource r = new Resource();
    r.setShortname("res");
    r.setCreator(creator);
    r.addManager(comanager);
    r.addManager(publisher);
    return r;
  }

  @Test
  void adminMayManageEverything() {
    assertThat(ResourceAccessPolicy.canManage(admin, resource())).isTrue();
    assertThat(ResourceAccessPolicy.canManage(admin, null)).isTrue();
  }

  @Test
  void creatorAndListedManagersMayManage() {
    assertThat(ResourceAccessPolicy.canManage(creator, resource())).isTrue();
    assertThat(ResourceAccessPolicy.canManage(comanager, resource())).isTrue();
    assertThat(ResourceAccessPolicy.canManage(publisher, resource())).isTrue();
  }

  @Test
  void othersMayNot() {
    assertThat(ResourceAccessPolicy.canManage(other, resource())).isFalse();
    assertThat(ResourceAccessPolicy.canManage(plain, resource())).isFalse();
    assertThat(ResourceAccessPolicy.canManage(other, null)).isFalse();
    assertThat(ResourceAccessPolicy.canManage(null, resource())).isFalse();
  }

  @Test
  void aCreatorWithoutManagerRightsMayNot() {
    Resource r = resource();
    r.setCreator(plain);
    assertThat(ResourceAccessPolicy.canManage(plain, r)).isFalse();
  }
}
