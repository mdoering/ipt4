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
package org.gbif.ipt.web.security;

import org.gbif.ipt.model.User;
import org.gbif.ipt.service.admin.UserAccountManager;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * IPT 3's RequireAdmin and RequireManager interceptors: the rights of the logged-in user's current record, so role
 * changes apply at once and deleted users lose access.
 */
final class IptAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

  private final UserAccountManager users;
  private final Predicate<User> rights;

  private IptAuthorization(UserAccountManager users, Predicate<User> rights) {
    this.users = users;
    this.rights = rights;
  }

  static IptAuthorization admin(UserAccountManager users) {
    return new IptAuthorization(users, User::hasAdminRights);
  }

  static IptAuthorization manager(UserAccountManager users) {
    return new IptAuthorization(users, User::hasManagerRights);
  }

  @Override
  public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
                                       RequestAuthorizationContext context) {
    Authentication auth = authentication.get();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return new AuthorizationDecision(false);
    }
    User user = users.get(auth.getName());
    return new AuthorizationDecision(user != null && rights.test(user));
  }
}
