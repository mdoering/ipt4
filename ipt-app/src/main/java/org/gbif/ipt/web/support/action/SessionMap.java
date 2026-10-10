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
package org.gbif.ipt.web.support.action;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** The HTTP session as the {@code Map<String, Object>} Struts gave actions; created when something is stored. */
final class SessionMap extends AbstractMap<String, Object> {

  private final HttpServletRequest request;

  SessionMap(HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public Object get(Object key) {
    HttpSession session = request.getSession(false);
    return session == null ? null : session.getAttribute(String.valueOf(key));
  }

  @Override
  public Object put(String key, Object value) {
    HttpSession session = request.getSession(true);
    Object old = session.getAttribute(key);
    session.setAttribute(key, value);
    return old;
  }

  @Override
  public Object remove(Object key) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }
    Object old = session.getAttribute(String.valueOf(key));
    session.removeAttribute(String.valueOf(key));
    return old;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return Collections.emptySet();
    }
    Set<Entry<String, Object>> entries = new HashSet<>();
    for (String name : Collections.list(session.getAttributeNames())) {
      entries.add(new SimpleEntry<>(name, session.getAttribute(name)));
    }
    return entries;
  }

  @Override
  public void clear() {
    HttpSession session = request.getSession(false);
    if (session != null) {
      for (String name : Collections.list(session.getAttributeNames())) {
        session.removeAttribute(name);
      }
    }
  }
}
