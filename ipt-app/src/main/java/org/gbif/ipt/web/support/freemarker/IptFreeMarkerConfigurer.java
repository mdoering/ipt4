package org.gbif.ipt.web.support.freemarker;

import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * FreeMarker settings copied from Struts 7.3 {@code FreemarkerManager.createConfiguration()}, so the IPT 3 templates
 * render unchanged. Differences: undefined variables fail the request (Struts printed HTML debug output into the page),
 * except in dev mode.
 */
public class IptFreeMarkerConfigurer extends FreeMarkerConfigurer {

  public static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

  private final boolean devMode;

  public IptFreeMarkerConfigurer(boolean devMode) {
    this.devMode = devMode;
  }

  @Override
  protected Configuration newConfiguration() {
    return new Configuration(FREEMARKER_VERSION);
  }

  @Override
  protected void postProcessConfiguration(Configuration config) {
    config.setObjectWrapper(new IptObjectWrapper(FREEMARKER_VERSION));
    config.setOutputFormat(HTMLOutputFormat.INSTANCE);
    config.setAutoEscapingPolicy(Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY);
    config.setLocalizedLookup(false);
    config.setWhitespaceStripping(true);
    config.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
    config.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
    config.setLogTemplateExceptions(false);
    config.setTemplateExceptionHandler(
        devMode ? TemplateExceptionHandler.HTML_DEBUG_HANDLER : TemplateExceptionHandler.RETHROW_HANDLER);
    config.setTemplateUpdateDelayMilliseconds(devMode ? 0 : 60_000);
  }
}
