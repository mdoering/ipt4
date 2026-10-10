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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import freemarker.template.TemplateModelException;

/** Directive parameters as Struts UI tags received them: known attributes plus pass-through ("dynamic") ones. */
final class TagAttributes {

  private final Map<?, ?> params;
  private final Set<String> known;

  TagAttributes(Map<?, ?> params, Set<String> known) {
    this.params = params;
    this.known = known;
  }

  String string(String name) throws TemplateModelException {
    return TemplateParams.string(params, name);
  }

  boolean bool(String name) throws TemplateModelException {
    return TemplateParams.bool(params, name);
  }

  Object raw(String name) {
    return params.get(name);
  }

  /** Attributes the tag does not interpret, written as they are (e.g. HTML5 {@code form} or {@code class}). */
  Map<String, String> dynamic() throws TemplateModelException {
    Map<String, String> dyn = new LinkedHashMap<>();
    for (Object key : params.keySet()) {
      String k = key.toString();
      if (!known.contains(k)) {
        dyn.put(k, TemplateParams.string(params, k));
      }
    }
    return dyn;
  }
}
