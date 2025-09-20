package org.codice.keip.flow.web.config;

import java.io.IOException;
import java.io.InputStream;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.FlowTranslator;
import org.codice.keip.flow.xml.GraphXmlParser;
import org.codice.keip.flow.xml.GraphXmlSerializer;
import org.codice.keip.flow.xml.spring.IntegrationGraphXmlParser;
import org.codice.keip.flow.xml.spring.IntegrationGraphXmlSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

@Configuration
public class FlowTranslatorConfig {

  private final NamespaceProps namespaceProps;

  public FlowTranslatorConfig(NamespaceProps namespaceProps) {
    this.namespaceProps = namespaceProps;
  }

  @Bean
  public FlowTranslator springIntegrationFlowTranslator() {
    GraphXmlSerializer serializer =
        new IntegrationGraphXmlSerializer(this.namespaceProps.namespaceMappings());

    GraphXmlParser parser;
    try {
      parser =
          new IntegrationGraphXmlParser(
              this.namespaceProps.namespaceMappings(),
              ComponentRegistry.fromJson(readComponentDefinitionJson()));

      parser.setValidationSchema(
          SchemaBuilder.buildSpringIntegrationSchemas(this.namespaceProps.namespaceMappings()));
    } catch (IOException | SAXException e) {
      throw new RuntimeException(e);
    }

    return new FlowTranslator(serializer, parser);
  }

  private static InputStream readComponentDefinitionJson() {
    // Included on the classpath by adding a dependency on the `eip-schema-definitions` artifact
    return FlowTranslatorConfig.class.getResourceAsStream("/springIntegrationEipComponents.json");
  }
}
