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

/** Struts {@code <@s.checkbox name fieldValue value cssClass disabled/>}; checked when {@code value} is true. */
final class CheckboxDirective implements TemplateDirectiveModel {

  private static final Set<String> KNOWN =
      Set.of("name", "id", "fieldValue", "value", "cssClass", "cssStyle", "disabled", "title", "tabindex");

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    TagAttributes a = new TagAttributes(params, KNOWN);
    String name = a.string("name");
    String fieldValue = a.string("fieldValue") != null ? a.string("fieldValue") : "true";
    String id = a.string("id") != null ? a.string("id") : HtmlTag.idFor(name);
    HtmlTag tag = new HtmlTag("input").attr("type", "checkbox").attr("name", name).attr("value", fieldValue)
        .flag("checked", a.bool("value")).flag("disabled", a.bool("disabled"))
        .attr("tabindex", a.string("tabindex")).attr("id", id)
        .attr("class", a.string("cssClass")).attr("style", a.string("cssStyle")).attr("title", a.string("title"))
        .attrs(a.dynamic());
    env.getOut().write(tag.selfClosing());
  }
}
