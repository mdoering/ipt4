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
import java.util.Set;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Struts {@code <@s.hidden name id value cssClass/>} (simple theme hidden.ftl); without {@code value} the value of the
 * {@code name} path, looked up like {@code s.property}. {@code required} has no markup in the simple theme.
 */
final class HiddenDirective implements TemplateDirectiveModel {

  private static final Set<String> KNOWN = Set.of("name", "id", "value", "cssClass", "cssStyle", "disabled", "required");

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    TagAttributes a = new TagAttributes(params, KNOWN);
    String name = a.string("name");
    String value = params.containsKey("value") ? a.string("value") : name == null ? null
        : PropertyDirective.lookup(env, name);
    String id = a.string("id") != null ? a.string("id") : HtmlTag.idFor(name);
    HtmlTag tag = new HtmlTag("input").attr("type", "hidden").attr("name", name).attr("value", value)
        .attr("id", id).attr("class", a.string("cssClass")).attr("style", a.string("cssStyle"))
        .flag("disabled", a.bool("disabled")).attrs(a.dynamic());
    env.getOut().write(tag.selfClosing());
  }
}
