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

/** Struts {@code <@s.file name id key accept cssClass/>} as the simple theme's file.ftl writes it. */
final class FileDirective implements TemplateDirectiveModel {

  private static final Set<String> KNOWN = Set.of("name", "id", "key", "accept", "size", "cssClass", "cssStyle",
      "disabled", "title", "tabindex");

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    TagAttributes a = new TagAttributes(params, KNOWN);
    // like Struts, key stands in for a missing name (in the simple theme it labels nothing)
    String name = a.string("name") != null ? a.string("name") : a.string("key");
    String id = a.string("id") != null ? a.string("id") : name != null ? HtmlTag.idFor(name) : null;
    HtmlTag tag = new HtmlTag("input").attr("type", "file").always("name", name)
        .attr("size", a.string("size")).flag("disabled", a.bool("disabled")).attr("accept", a.string("accept"))
        .attr("tabindex", a.string("tabindex")).attr("id", id)
        .attr("class", a.string("cssClass")).attr("style", a.string("cssStyle")).attr("title", a.string("title"))
        .attrs(a.dynamic());
    env.getOut().write(tag.selfClosing());
  }
}
