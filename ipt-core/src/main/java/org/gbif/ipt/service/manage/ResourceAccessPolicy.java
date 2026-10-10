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

import javax.annotation.Nullable;

/** Who may manage a resource. The rule of IPT 3's {@code RequireManagerInterceptor.isAuthorized}, unchanged. */
public final class ResourceAccessPolicy {

  private ResourceAccessPolicy() {
  }

  public static boolean canManage(@Nullable User user, @Nullable Resource resource) {
    if (user == null) {
      return false;
    }
    if (user.hasAdminRights()) {
      return true;
    }
    if (resource != null && user.hasManagerRights()) {
      // even resource creators need still to be managers
      if (user.equals(resource.getCreator())) {
        return true;
      }
      for (User m : resource.getManagers()) {
        if (user.equals(m)) {
          return true;
        }
      }
    }
    return false;
  }
}
