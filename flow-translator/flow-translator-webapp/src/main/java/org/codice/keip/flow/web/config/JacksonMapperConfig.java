package org.codice.keip.flow.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonMapperConfig {

  @Bean
  public JsonMapper objectMapper() {
    return JsonMapper.builder()
        // Exclude nulls
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        // Ignore unknown properties
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // Accept case-insensitive enums
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        // Write enums in lowercase
        .configure(EnumFeature.WRITE_ENUMS_TO_LOWERCASE, true)
        .build();
  }
}
