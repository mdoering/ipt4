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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;

/**
 * Struts {@code <@s.select name list listKey listValue value emptyOption size disabled/>}: options from a map
 * (key/value) or a collection (bean properties {@code listKey}/{@code listValue}); the option whose key equals
 * {@code value} is selected.
 */
final class SelectDirective implements TemplateDirectiveModel {

  private static final Set<String> KNOWN = Set.of("name", "id", "list", "listKey", "listValue", "value", "size",
      "disabled", "emptyOption", "headerKey", "headerValue", "cssClass", "cssStyle", "title", "tabindex", "multiple");

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    TagAttributes a = new TagAttributes(params, KNOWN);
    String name = a.string("name");
    String id = a.string("id") != null ? a.string("id") : HtmlTag.idFor(name);
    String selected = a.raw("value") == null ? null : String.valueOf(DeepUnwrap.unwrap((TemplateModel) a.raw("value")));
    // Struts evaluated a string value as an expression (value="organisation.key"); one naming nothing is literal
    if (a.raw("value") != null && DeepUnwrap.unwrap((TemplateModel) a.raw("value")) instanceof String) {
      String evaluated = PropertyDirective.lookup(env, selected);
      if (evaluated != null && !evaluated.isEmpty()) {
        selected = evaluated;
      }
    }
    StringBuilder sb = new StringBuilder(new HtmlTag("select").attr("name", name == null ? "" : name)
        .attr("size", a.string("size")).flag("disabled", a.bool("disabled")).attr("tabindex", a.string("tabindex"))
        .attr("id", id).attr("class", a.string("cssClass")).attr("style", a.string("cssStyle"))
        .attr("title", a.string("title")).flag("multiple", a.bool("multiple")).attrs(a.dynamic()).open());
    sb.append('\n');
    if (a.string("headerKey") != null && a.string("headerValue") != null) {
      option(sb, a.string("headerKey"), a.string("headerValue"), selected);
    }
    if (a.bool("emptyOption")) {
      sb.append("    <option value=\"\"></option>\n");
    }
    for (Map.Entry<String, String> o : options(env, a).entrySet()) {
      option(sb, o.getKey(), o.getValue(), selected);
    }
    env.getOut().write(sb.append("</select>").toString());
  }

  private static void option(StringBuilder sb, String key, String label, String selected) {
    sb.append(new HtmlTag("option").attr("value", key == null ? "" : key)
        .flag("selected", Objects.equals(key, selected)).open())
        .append(HtmlTag.escape(label)).append("</option>\n");
  }

  private static Map<String, String> options(Environment env, TagAttributes a) throws TemplateException {
    TemplateModel model = (TemplateModel) a.raw("list");
    // Struts evaluated a string list as an expression (list="organisations")
    if (model != null && DeepUnwrap.unwrap(model) instanceof String expression) {
      model = PropertyDirective.lookupModel(env, expression);
    }
    Object list = model == null ? null : DeepUnwrap.unwrap(model);
    Map<String, String> options = new LinkedHashMap<>();
    if (list instanceof Map<?, ?> map) {
      map.forEach((k, v) -> options.put(String.valueOf(k), v == null ? "" : String.valueOf(v)));
    } else if (list instanceof Collection<?> items) {
      String keyProp = a.string("listKey");
      String valueProp = a.string("listValue");
      for (Object item : items) {
        String key = keyProp == null ? String.valueOf(item) : property(item, keyProp);
        String value = valueProp == null ? String.valueOf(item) : property(item, valueProp);
        options.put(key, value);
      }
    }
    return options;
  }

  private static String property(Object bean, String property) throws TemplateModelException {
    if (bean == null) {
      return "";
    }
    String cap = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    for (String getter : new String[] {"get" + cap, "is" + cap, property}) {
      try {
        Method m = bean.getClass().getMethod(getter);
        Object v = m.invoke(bean);
        return v == null ? "" : String.valueOf(v);
      } catch (NoSuchMethodException e) {
        // next accessor name
      } catch (ReflectiveOperationException e) {
        throw new TemplateModelException("Cannot read " + property + " of " + bean.getClass().getName(), e);
      }
    }
    if (bean instanceof Map<?, ?> m) {
      return String.valueOf(m.get(property));
    }
    return "";
  }
}
