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

import java.util.List;

import freemarker.core.Environment;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateHTMLOutputModel;
import freemarker.core.TemplateMarkupOutputModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/**
 * {@code t(key, args...)}: the text for a message key in the request's locale, as HTML markup.
 *
 * <p>Message texts are written as they are (some contain links), as Struts' {@code s.text} did. Plain-text arguments
 * are HTML-escaped and numbers formatted like {@code ${n}}, as they were inside {@code s.param}; markup arguments (a
 * captured {@code <#assign>} or another {@code t(...)}) are inserted as they are. {@code t(key)?markup_string} gives
 * the text as plain text, escaped on output.
 */
final class TextFunction implements TemplateMethodModelEx {

  private final Messages messages;

  TextFunction(Messages messages) {
    this.messages = messages;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public TemplateHTMLOutputModel exec(List arguments) throws TemplateModelException {
    if (arguments.isEmpty() || !(arguments.get(0) instanceof TemplateScalarModel key)) {
      throw new TemplateModelException("t(key, args...) needs a message key");
    }
    Environment env = Environment.getCurrentEnvironment();
    Object[] args = new Object[arguments.size() - 1];
    for (int i = 1; i < arguments.size(); i++) {
      args[i - 1] = argument(env, (TemplateModel) arguments.get(i), i);
    }
    String text = messages.getOrDefault(env.getLocale(), key.getAsString(), null, args);
    return HTMLOutputFormat.INSTANCE.fromMarkup(text);
  }

  private static String argument(Environment env, TemplateModel arg, int position) throws TemplateModelException {
    if (arg instanceof TemplateMarkupOutputModel<?> markup) {
      return markupString(markup);
    }
    if (arg instanceof TemplateScalarModel scalar) {
      return HTMLOutputFormat.INSTANCE.escapePlainText(scalar.getAsString());
    }
    if (arg instanceof TemplateNumberModel number) {
      try {
        return HTMLOutputFormat.INSTANCE.escapePlainText(env.getTemplateNumberFormat().formatToPlainText(number));
      } catch (freemarker.core.TemplateValueFormatException e) {
        throw new TemplateModelException("Cannot format argument " + position + " of t()", e);
      }
    }
    if (arg == null) {
      throw new TemplateModelException("Argument " + position + " of t() is missing; use x! for optional values");
    }
    throw new TemplateModelException("Argument " + position + " of t() must be a string, number or markup; use ?string");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static String markupString(TemplateMarkupOutputModel markup) throws TemplateModelException {
    return markup.getOutputFormat().getMarkupString(markup);
  }
}
