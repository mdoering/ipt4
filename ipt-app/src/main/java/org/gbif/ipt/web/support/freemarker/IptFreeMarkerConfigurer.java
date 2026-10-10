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

import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

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
