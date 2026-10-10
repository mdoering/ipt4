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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * IPT 3's LoginAction check: trimmed email and password against the IPT users (including the simplified "admin"
 * login). The principal is the user's email; rights are read from the user record on every request (see
 * {@link IptAuthorization}), as IPT 3 kept the live user object in the session.
 */
@Component
public class IptAuthenticationProvider implements AuthenticationProvider {

  private final UserAccountManager users;

  public IptAuthenticationProvider(UserAccountManager users) {
    this.users = users;
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    String email = StringUtils.trimToEmpty(authentication.getName());
    String password = StringUtils.trimToEmpty(String.valueOf(authentication.getCredentials()));
    User user = email.isEmpty() ? null : users.authenticate(email, password);
    if (user == null) {
      throw new BadCredentialsException("Bad credentials");
    }
    return UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase())));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
