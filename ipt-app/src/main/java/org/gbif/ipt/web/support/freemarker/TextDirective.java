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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Struts {@code <@s.text name="key">}: writes the localised text unescaped, as Struts did. Nested {@code s.param}
 * bodies are the message arguments; other non-blank body text is the default message. {@code var} stores the text
 * in a template variable instead of writing it.
 */
final class TextDirective implements TemplateDirectiveModel {

  private final Messages messages;

  TextDirective(Messages messages) {
    this.messages = messages;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    String name = TemplateParams.string(params, "name");
    if (name == null) {
      throw new TemplateModelException("s.text requires a name");
    }
    List<String> args = new ArrayList<>();
    StringWriter bodyText = new StringWriter();
    Deque<List<String>> stack = ParamStack.of(env);
    stack.push(args);
    try {
      if (body != null) {
        body.render(bodyText);
      }
    } finally {
      stack.pop();
    }
    String defaultMessage = bodyText.toString().isBlank() ? null : bodyText.toString();
    String text = messages.getOrDefault(env.getLocale(), name, defaultMessage, args.toArray());
    if (TemplateParams.bool(params, "escapeHtml")) {
      text = StringEscapeUtils.escapeHtml4(text);
    }
    if (TemplateParams.bool(params, "escapeJavaScript")) {
      text = StringEscapeUtils.escapeEcmaScript(text);
    }
    if (TemplateParams.bool(params, "escapeXml")) {
      text = StringEscapeUtils.escapeXml10(text);
    }
    String var = TemplateParams.string(params, "var");
    if (var != null) {
      env.setVariable(var, new SimpleScalar(text));
    } else {
      env.getOut().write(text);
    }
  }
}
