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

import org.apache.commons.text.StringEscapeUtils;

/** Writes an HTML start tag with escaped attributes, in the attribute order the Struts simple theme used. */
final class HtmlTag {

  private final String name;
  private final Map<String, String> attributes = new LinkedHashMap<>();

  HtmlTag(String name) {
    this.name = name;
  }

  HtmlTag attr(String key, String value) {
    if (value != null && !value.isEmpty()) {
      attributes.put(key, value);
    }
    return this;
  }

  HtmlTag flag(String key, boolean on) {
    if (on) {
      attributes.put(key, key);
    }
    return this;
  }

  HtmlTag attrs(Map<String, String> more) {
    more.forEach(this::attr);
    return this;
  }

  String open() {
    StringBuilder sb = new StringBuilder("<").append(name);
    attributes.forEach((k, v) -> sb.append(' ').append(k).append("=\"").append(escape(v)).append('"'));
    return sb.append('>').toString();
  }

  String selfClosing() {
    String open = open();
    return open.substring(0, open.length() - 1) + "/>";
  }

  static String escape(String text) {
    return text == null ? "" : StringEscapeUtils.escapeHtml4(text);
  }

  /** Struts' {@code UIBean.escape}: ids are names with every character outside [a-zA-Z0-9_] replaced by '_'. */
  static String idFor(String name) {
    return name == null ? null : name.replaceAll("[^a-zA-Z0-9_]", "_");
  }
}
