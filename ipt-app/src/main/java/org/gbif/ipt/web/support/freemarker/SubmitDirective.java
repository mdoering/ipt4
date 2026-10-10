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

import org.gbif.ipt.i18n.Messages;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Struts {@code <@s.submit name="save" key="button.save" cssClass="..."/>}: an {@code input type="submit"} labelled
 * with the text of {@code key} (or {@code value}); {@code type="button"} gives a {@code button} element.
 */
final class SubmitDirective implements TemplateDirectiveModel {

  private static final Set<String> KNOWN =
      Set.of("name", "key", "value", "id", "cssClass", "cssStyle", "disabled", "type", "title", "tabindex");

  private final Messages messages;

  SubmitDirective(Messages messages) {
    this.messages = messages;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    TagAttributes a = new TagAttributes(params, KNOWN);
    String name = a.string("name");
    String label = a.string("value");
    if (label == null && a.string("key") != null) {
      label = messages.get(env.getLocale(), a.string("key"));
    }
    String id = a.string("id") != null ? a.string("id") : HtmlTag.idFor(name);
    boolean button = "button".equals(a.string("type"));
    HtmlTag tag = new HtmlTag(button ? "button" : "input").attr("type", "submit");
    if (!button) {
      tag.attr("value", label);
    }
    tag.attr("id", id).attr("name", name).flag("disabled", a.bool("disabled"))
        .attr("class", a.string("cssClass")).attr("style", a.string("cssStyle"))
        .attr("title", a.string("title")).attr("tabindex", a.string("tabindex"))
        .attrs(a.dynamic());
    if (button) {
      env.getOut().write(tag.open() + HtmlTag.escape(label) + "</button>");
    } else {
      env.getOut().write(tag.selfClosing());
    }
  }
}
