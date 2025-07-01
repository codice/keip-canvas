package org.codice.keip.flow.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FlowTranslatorWebApplication {

  public static void main(String[] args) {
    SpringApplication.run(FlowTranslatorWebApplication.class, args);
  }
}
