package org.codice.keip.flow.web.cors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Handles CORS configuration for FlowTranslator application endpoints only. CORS settings for
// Actuator endpoints need to be configured separately.
@Configuration
public class CorsConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(CorsConfiguration.class);

  private final CorsProps corsProps;

  public CorsConfiguration(CorsProps corsProps) {
    this.corsProps = corsProps;
  }

  @Bean
  @ConditionalOnProperty(prefix = "web.cors", name = "enabled", havingValue = "true")
  public WebMvcConfigurer configureCorsMapping() {
    LOGGER.info("Enable CORS configuration");
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration reg = registry.addMapping("/**");
        if (corsProps.allowedOrigins() != null) {
          LOGGER.info(
              "Setting CORS header 'Access-Control-Allow-Origin' to [{}]",
              String.join(",", corsProps.allowedOrigins()));
          reg.allowedOrigins(corsProps.allowedOrigins());
        }
        if (corsProps.allowedMethods() != null) {
          LOGGER.info(
              "Setting CORS header 'Access-Control-Allow-Methods' to [{}]",
              String.join(",", corsProps.allowedMethods()));
          reg.allowedMethods(corsProps.allowedMethods());
        }
      }
    };
  }
}
