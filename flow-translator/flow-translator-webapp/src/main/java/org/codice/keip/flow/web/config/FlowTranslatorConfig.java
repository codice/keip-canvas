package org.codice.keip.flow.web.config;

import org.codice.keip.flow.FlowTranslator;
import org.codice.keip.flow.xml.GraphTransformer;
import org.codice.keip.flow.xml.spring.IntegrationGraphTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowTranslatorConfig {

  private final NamespaceProps namespaceProps;

  public FlowTranslatorConfig(NamespaceProps namespaceProps) {
    this.namespaceProps = namespaceProps;
  }

  @Bean
  public FlowTranslator springIntegrationFlowTranslator() {
    GraphTransformer integrationTransformer =
        new IntegrationGraphTransformer(this.namespaceProps.namespaceMappings());
    return new FlowTranslator(integrationTransformer);
  }
}
