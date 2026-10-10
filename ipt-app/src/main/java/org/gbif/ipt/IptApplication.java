package org.gbif.ipt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Lives in the root package so every test below {@code org.gbif.ipt} finds it as its Spring Boot configuration. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IptApplication {

  public static void main(String[] args) {
    SpringApplication.run(IptApplication.class, args);
  }
}
