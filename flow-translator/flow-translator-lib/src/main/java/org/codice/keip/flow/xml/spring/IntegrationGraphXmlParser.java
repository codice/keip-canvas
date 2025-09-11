package org.codice.keip.flow.xml.spring;

import java.util.Collection;
import java.util.Set;
import javax.xml.namespace.QName;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.xml.GraphEdgeBuilder;
import org.codice.keip.flow.xml.GraphXmlParser;
import org.codice.keip.flow.xml.NamespaceSpec;
import org.codice.keip.flow.xml.XmlElementTransformer;

public class IntegrationGraphXmlParser extends GraphXmlParser {

  private final XmlElementTransformer xmlElementTransformer;

  private static final Set<String> CUSTOM_ENTITY_NAMESPACES =
      Set.of(Namespaces.BEANS.xmlNamespace());

  public IntegrationGraphXmlParser(
      Collection<NamespaceSpec> namespaceSpecs, ComponentRegistry registry) {
    super(namespaceSpecs, registry);
    xmlElementTransformer = new DefaultXmlElementTransformer();
  }

  public IntegrationGraphXmlParser(
      Collection<NamespaceSpec> namespaceSpecs,
      ComponentRegistry registry,
      XmlElementTransformer customTransformer) {
    super(namespaceSpecs, registry);
    xmlElementTransformer = customTransformer;
  }

  @Override
  protected QName rootElement() {
    return new QName(Namespaces.BEANS.xmlNamespace(), Namespaces.BEANS.eipNamespace());
  }

  @Override
  protected boolean isCustomEntity(QName name) {
    return CUSTOM_ENTITY_NAMESPACES.contains(name.getNamespaceURI());
  }

  @Override
  protected XmlElementTransformer getXmlElementTransformer() {
    return xmlElementTransformer;
  }

  @Override
  protected GraphEdgeBuilder graphEdgeBuilder() {
    return (Collection<EipNode> nodes) -> new ChannelEdgeBuilder(nodes).buildGraph();
  }
}
