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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.utility.DeepUnwrap;

/**
 * Struts {@code <@s.fielderror fieldName="x" id cssClass/>}: the validation errors of a form field as a list, or
 * nothing. Errors come from the {@code fieldErrors} template variable ({@code Map<String, List<String>>}).
 */
final class FieldErrorDirective implements TemplateDirectiveModel {

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    String field = TemplateParams.string(params, "fieldName");
    TemplateModel model = env.getVariable("fieldErrors");
    if (field == null || model == null) {
      return;
    }
    Object errors = DeepUnwrap.unwrap(model);
    if (!(errors instanceof Map<?, ?> map) || !(map.get(field) instanceof Collection<?> messages) || messages.isEmpty()) {
      return;
    }
    String cssClass = TemplateParams.string(params, "cssClass");
    StringBuilder sb = new StringBuilder(new HtmlTag("ul").attr("id", TemplateParams.string(params, "id"))
        .attr("class", cssClass == null ? "errorMessage" : cssClass)
        .attr("style", TemplateParams.string(params, "cssStyle")).open());
    for (Object message : List.copyOf(messages)) {
      sb.append("<li><span>").append(HtmlTag.escape(String.valueOf(message))).append("</span></li>");
    }
    env.getOut().write(sb.append("</ul>").toString());
  }
}
