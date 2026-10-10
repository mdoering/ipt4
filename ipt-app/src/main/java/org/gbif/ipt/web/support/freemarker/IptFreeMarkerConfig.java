package org.gbif.ipt.web.support.freemarker;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.gbif.ipt.app.IptProperties;
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
