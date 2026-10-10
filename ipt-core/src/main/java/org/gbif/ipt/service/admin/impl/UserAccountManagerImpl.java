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
package org.gbif.ipt.service.admin.impl;

import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.User.Role;
import org.gbif.ipt.service.AlreadyExistingException;
import org.gbif.ipt.service.BaseManager;
import org.gbif.ipt.service.DeletionNotAllowedException;
import org.gbif.ipt.service.DeletionNotAllowedException.Reason;
import org.gbif.ipt.service.InvalidConfigException;
import org.gbif.ipt.service.InvalidConfigException.TYPE;
import org.gbif.ipt.service.admin.UserAccountManager;
import org.gbif.ipt.utils.IptFileUtils;
import org.gbif.ipt.persistence.UserStore;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Strings;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Reads user accounts from {@code config/users.json}.
 */
public class UserAccountManagerImpl extends BaseManager implements UserAccountManager {

  private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

  public static final String PERSISTENCE_FILE = UserStore.FILENAME;
  private final Map<String, User> users = new LinkedHashMap<>();
  private boolean allowSimplifiedAdminLogin = true;
  private String onlyAdminEmail;
  private final UserStore store;

  private User setupUser;

  @Inject
  public UserAccountManagerImpl(
      AppConfig cfg,
      DataDir dataDir) {
    super(cfg, dataDir);
    this.store = new UserStore(dataDir.configFile(PERSISTENCE_FILE).toPath());
  }

  private User addUser(User user) {
    if (user != null) {
      if (user.getRole() == Role.Admin) {
        LOG.debug("Adding admin {}", user.getEmail());
        if (allowSimplifiedAdminLogin) {
          if (onlyAdminEmail == null || users.isEmpty()) {
            // first admin - keep its email address available for simplified login
            // without email address but keyword "admin"
            onlyAdminEmail = user.getEmail();
          } else {
            // at least 2 admins exist - disable simplified admin login
            onlyAdminEmail = null;
            allowSimplifiedAdminLogin = false;
          }
        }
      } else {
        LOG.debug("Adding user {}", user.getEmail());
      }
      users.put(user.getEmail().toLowerCase(), user);
    }
    return user;
  }

  @Override
  public User authenticate(String email, String password) {
    if (allowSimplifiedAdminLogin && email != null && email.equalsIgnoreCase("admin")) {
      // lookup the admins email address#
      email = onlyAdminEmail;
    }
    User agent = get(email);
    if (agent != null && agent.getPassword() != null
        && BCRYPT.matches(password, agent.getPassword())) {
      return agent;
    }
    return null;
  }

  @Override
  public void create(User user) throws AlreadyExistingException, IOException {
    if (user != null) {
      if (get(user.getEmail()) != null) {
        throw new AlreadyExistingException();
      }
      // hash password before creation
      String hash = BCRYPT.encode(user.getPassword());
      user.setPassword(hash);
      addUser(user);
      save();
    }
  }

  @Override
  public boolean delete(String email) throws DeletionNotAllowedException, IOException {
    if (email != null) {
      User remUser = get(email);
      if (remUser != null) {

        // when deleting an admin, ensure another admin still exists
        if (remUser.getRole() == Role.Admin) {
          boolean lastAdmin = true;
          for (User u : users.values()) {
            if (u.getRole() == Role.Admin && !u.equals(remUser)) {
              lastAdmin = false;
              break;
            }
          }
          if (lastAdmin) {
            LOG.warn("Last admin cannot be deleted");
            throw new DeletionNotAllowedException(Reason.LAST_ADMIN);
          }
        }

        boolean remove = remove(email);

        if (remove) {
          save();
        }

        return remove;
      }
    }
    return false;
  }

  /**
   * Retrieve user from internal hash.
   *
   * @param email email of user account to retrieve
   * @return User if found, null otherwise
   */
  @Override
  public User get(String email) {
    if (email != null && users.containsKey(email.toLowerCase())) {
      return users.get(email.toLowerCase());
    }
    return null;
  }

  /**
   * Remove user from internal hash.
   *
   * @param email email of user account to remove
   * @return true if user was removed, false otherwise
   */
  public boolean remove(String email) {
    if (email != null && users.containsKey(email.toLowerCase())) {
      return users.remove(email.toLowerCase()) != null;
    }
    return false;
  }

  @Override
  public String getDefaultAdminEmail() {
    return cfg.getAdminEmail();
  }

  @Override
  public User getSetupUser() {
    return setupUser;
  }

  @Override
  public List<User> list() {
    ArrayList<User> userList = new ArrayList<>(users.values());
    userList.sort(Comparator.comparing(o -> (o.getFirstname() + " " + o.getLastname())));
    return userList;
  }

  @Override
  public List<User> list(Role role) {
    List<User> matchingUsers = new ArrayList<>();
    for (User u : users.values()) {
      if (u.getRole() == role) {
        matchingUsers.add(u);
      }
    }
    return matchingUsers;
  }

  @Override
  public void load() throws InvalidConfigException {
    try {
      users.clear();
      for (User u : store.load()) {
        addUser(u);
      }
      if (users.isEmpty()) {
        LOG.warn("No user accounts in {} (This is normal when first setting up a new datadir)", PERSISTENCE_FILE);
      }
    } catch (IOException | RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw new InvalidConfigException(TYPE.USER_CONFIG, "Couldn't read user accounts: " + e.getMessage());
    }
  }

  @Override
  public synchronized void save() throws IOException {
    LOG.debug("Saving all {} user accounts...", users.size());
    store.save(new ArrayList<>(users.values()));
  }


  @Override
  public void save(User user) throws IOException {
    addUser(user);
    save();
  }

  @Override
  public void setSetupUser(User setupUser) {
    this.setupUser = setupUser;
  }

}
