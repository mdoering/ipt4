package org.gbif.ipt.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "org.gbif.ipt")
@ConfigurationPropertiesScan("org.gbif.ipt")
public class IptApplication {

  public static void main(String[] args) {
    SpringApplication.run(IptApplication.class, args);
  }
}
