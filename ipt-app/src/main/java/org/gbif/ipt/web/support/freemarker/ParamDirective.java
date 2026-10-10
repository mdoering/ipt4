package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/** Struts {@code <@s.param>body</@s.param>}: adds its rendered body as an argument of the enclosing {@code s.text}. */
final class ParamDirective implements TemplateDirectiveModel {

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {
    StringWriter value = new StringWriter();
    if (body != null) {
      body.render(value);
    }
    List<String> args = ParamStack.of(env).peek();
    if (args != null) {
      args.add(value.toString());
    }
  }
}
