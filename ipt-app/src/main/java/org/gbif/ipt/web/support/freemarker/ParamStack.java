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
package org.gbif.ipt.web.support.freemarker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import freemarker.core.Environment;

/** Collects the {@code s.param} values of the innermost enclosing {@code s.text} within one template rendering. */
final class ParamStack {

  private static final Object KEY = new Object();

  private ParamStack() {}

  @SuppressWarnings("unchecked")
  static Deque<List<String>> of(Environment env) {
    Deque<List<String>> stack = (Deque<List<String>>) env.getCustomState(KEY);
    if (stack == null) {
      stack = new ArrayDeque<>();
      env.setCustomState(KEY, stack);
    }
    return stack;
  }
}
