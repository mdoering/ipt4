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

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.core.Environment;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Struts {@code <@s.property value="eml.title"/>}: writes the (HTML-escaped) value of a dotted property path, looked
 * up like the value stack did: model, then page bean. {@code %{...}} wrappers are accepted. Missing values print
 * nothing.
 */
final class PropertyDirective implements TemplateDirectiveModel {

  private static final Pattern STEP =
      Pattern.compile("\\.?([A-Za-z_$][\\w$]*)|\\[(\\d+)]|\\['([^']*)']|\\[\"([^\"]*)\"]");

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    String path = TemplateParams.string(params, "value");
    if (path == null) {
      return;
    }
    path = path.trim();
    if (path.startsWith("%{") && path.endsWith("}")) {
      path = path.substring(2, path.length() - 1).trim();
    }
    String text = lookup(env, path);
    if (text == null) {
      return;
    }
    boolean escape = params.get("escapeHtml") == null || TemplateParams.bool(params, "escapeHtml");
    env.getOut().write(escape ? HtmlTag.escape(text) : text);
  }

  /** The value of an OGNL-style path (names, {@code [index]}, {@code ['key']}) as text, or null when missing. */
  static String lookup(Environment env, String path) throws TemplateException {
    TemplateModel value = null;
    Matcher step = STEP.matcher(path);
    boolean first = true;
    while (step.find()) {
      String name = step.group(1);
      String index = step.group(2);
      String key = step.group(3) != null ? step.group(3) : step.group(4);
      if (name != null) {
        value = first ? env.getVariable(name) : value instanceof TemplateHashModel hash ? hash.get(name) : null;
      } else if (index != null) {
        int i = Integer.parseInt(index);
        value = value instanceof TemplateSequenceModel seq && i < seq.size() ? seq.get(i) : null;
      } else if (key != null) {
        value = value instanceof TemplateHashModel hash ? hash.get(key) : null;
      }
      first = false;
      if (value == null) {
        return null;
      }
    }
    return first ? null : asString(value);
  }

  private static String asString(TemplateModel value) throws TemplateException {
    if (value instanceof TemplateScalarModel s) {
      return s.getAsString();
    }
    if (value instanceof TemplateNumberModel n) {
      return n.getAsNumber().toString();
    }
    if (value instanceof TemplateBooleanModel b) {
      return Boolean.toString(b.getAsBoolean());
    }
    if (value instanceof TemplateDateModel d) {
      return d.getAsDate().toString();
    }
    return String.valueOf(freemarker.template.utility.DeepUnwrap.unwrap(value));
  }
}
