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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;

import freemarker.template.SimpleHash;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gives IPT 3 templates the variable lookup of the Struts value stack: anything not in the model is read from the page
 * bean (model attribute {@value #PAGE}), which templates can also address as {@code action}.
 */
public class IptFreeMarkerView extends FreeMarkerView {

  public static final String PAGE = "page";
  public static final String ACTION = "action";

  /**
   * Spring Security creates the CSRF token, and with it the session, when a form first asks for it. On a long page
   * the servlet container has sent the start of the response by then, and a session can no longer be created.
   */
  @Override
  protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
    if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken csrf) {
      csrf.getToken();
    }
    super.renderMergedTemplateModel(model, request, response);
  }

  @Override
  protected SimpleHash buildTemplateModel(
      Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
    Object page = model.get(PAGE);
    Map<String, Object> templateModel = model;
    if (page != null && !model.containsKey(ACTION)) {
      templateModel = new LinkedHashMap<>(model);
      templateModel.put(ACTION, page);
    }
    SimpleHash base = super.buildTemplateModel(templateModel, request, response);
    return page == null ? base
        : new PageFallbackHash(base, java.util.Set.copyOf(templateModel.keySet()), page, getObjectWrapper());
  }
}
