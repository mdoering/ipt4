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

import org.gbif.ipt.app.IptProperties;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * Replaces Spring Boot's FreeMarker beans, so {@code spring.freemarker.*} properties do not apply; all settings are in
 * {@link IptFreeMarkerConfigurer}.
 */
@Configuration(proxyBeanMethods = false)
public class IptFreeMarkerConfig {

  public static final String TEMPLATE_PATH = "classpath:/templates/";

  @Bean
  public FreeMarkerConfigurer freeMarkerConfigurer(
      IptProperties props, ObjectProvider<FreeMarkerSharedVariables> sharedVariables) {
    IptFreeMarkerConfigurer configurer = new IptFreeMarkerConfigurer(props.devMode());
    configurer.setTemplateLoaderPath(TEMPLATE_PATH);
    configurer.setDefaultEncoding(StandardCharsets.UTF_8.name());
    configurer.setPreferFileSystemAccess(false);
    Map<String, Object> variables = new HashMap<>();
    sharedVariables.orderedStream().forEach(v -> variables.putAll(v.variables()));
    configurer.setFreemarkerVariables(variables);
    return configurer;
  }

  @Bean(name = "freeMarkerViewResolver")
  public FreeMarkerViewResolver freeMarkerViewResolver() {
    FreeMarkerViewResolver resolver = new FreeMarkerViewResolver("", ".ftl");
    resolver.setViewClass(IptFreeMarkerView.class);
    resolver.setContentType("text/html;charset=UTF-8");
    resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
    return resolver;
  }
}
