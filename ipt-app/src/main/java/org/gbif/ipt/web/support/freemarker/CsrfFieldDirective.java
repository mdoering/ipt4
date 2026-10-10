package org.gbif.ipt.web.support.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.HtmlUtils;

/** {@code <@csrfField/>}: the hidden CSRF token input every POST form needs. */
public final class CsrfFieldDirective implements TemplateDirectiveModel {

  @Override
  @SuppressWarnings("rawtypes")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws IOException {
    CsrfToken token = currentToken();
    if (token == null) {
      return;
    }
    env.getOut()
        .write(
            "<input type=\"hidden\" name=\""
                + HtmlUtils.htmlEscape(token.getParameterName())
                + "\" value=\""
                + HtmlUtils.htmlEscape(token.getToken())
                + "\"/>");
  }

  private static CsrfToken currentToken() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servlet) {
      return (CsrfToken) servlet.getRequest().getAttribute(CsrfToken.class.getName());
    }
    return null;
  }
}
