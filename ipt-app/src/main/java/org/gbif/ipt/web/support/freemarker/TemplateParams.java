package org.gbif.ipt.web.support.freemarker;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import java.util.Map;

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
