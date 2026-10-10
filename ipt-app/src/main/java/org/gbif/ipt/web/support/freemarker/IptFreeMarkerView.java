package org.gbif.ipt.web.support.freemarker;

import freemarker.template.SimpleHash;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.servlet.view.freemarker.FreeMarkerView;

/**
 * Gives IPT 3 templates the variable lookup of the Struts value stack: anything not in the model is read from the page
 * bean (model attribute {@value #PAGE}), which templates can also address as {@code action}.
 */
public class IptFreeMarkerView extends FreeMarkerView {

  public static final String PAGE = "page";
  public static final String ACTION = "action";

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
    return page == null ? base : new PageFallbackHash(base, page, getObjectWrapper());
  }
}
