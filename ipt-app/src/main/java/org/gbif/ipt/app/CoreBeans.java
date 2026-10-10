package org.gbif.ipt.app;

import org.gbif.ipt.i18n.BundleMessages;
import org.gbif.ipt.i18n.Messages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans for ipt-core classes, which carry no Spring annotations of their own. */
@Configuration(proxyBeanMethods = false)
public class CoreBeans {

  @Bean
  public Messages messages() {
    return new BundleMessages();
  }
}
