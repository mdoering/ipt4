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

import java.util.Map;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

/** Reads directive parameters the way Struts tags accepted them (strings, numbers or booleans). */
final class TemplateParams {

  private TemplateParams() {}

  static String string(Map<?, ?> params, String name) throws TemplateModelException {
    Object value = params.get(name);
    if (value == null) {
      return null;
    }
    if (value instanceof TemplateScalarModel scalar) {
      return scalar.getAsString();
    }
    if (value instanceof TemplateNumberModel number) {
      return number.getAsNumber().toString();
    }
    if (value instanceof TemplateBooleanModel bool) {
      return Boolean.toString(bool.getAsBoolean());
    }
    throw new TemplateModelException("Parameter " + name + " must be a string");
  }

  static boolean bool(Map<?, ?> params, String name) throws TemplateModelException {
    Object value = params.get(name);
    if (value == null) {
      return false;
    }
    if (value instanceof TemplateBooleanModel bool) {
      return bool.getAsBoolean();
    }
    if (value instanceof TemplateScalarModel scalar) {
      return Boolean.parseBoolean(scalar.getAsString());
    }
    throw new TemplateModelException("Parameter " + name + " must be a boolean");
  }
}
