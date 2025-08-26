package org.codice.keip.flow.xml.spring;

import java.util.Collection;
import java.util.Set;
import javax.xml.namespace.QName;
import org.codice.keip.flow.xml.GraphXmlSerializer;
import org.codice.keip.flow.xml.NamespaceSpec;
import org.codice.keip.flow.xml.NodeTransformer;

public final class IntegrationGraphXmlSerializer extends GraphXmlSerializer {

  private final NodeTransformer nodeTransformer;

  public IntegrationGraphXmlSerializer(Collection<NamespaceSpec> namespaceSpecs) {
    super(namespaceSpecs);
    this.nodeTransformer = new DefaultNodeTransformer();
  }

  public IntegrationGraphXmlSerializer(
      Collection<NamespaceSpec> namespaceSpecs, NodeTransformer customNodeTransformer) {
    super(namespaceSpecs);
    this.nodeTransformer = customNodeTransformer;
  }

  @Override
  protected NamespaceSpec defaultNamespace() {
    return new NamespaceSpec(
        Namespaces.BEANS.eipNamespace(),
        Namespaces.BEANS.xmlNamespace(),
        Namespaces.BEANS.schemaLocation());
  }

  @Override
  protected Set<NamespaceSpec> requiredNamespaces() {
    return Set.of(Namespaces.INTEGRATION);
  }

  @Override
  protected QName rootElement() {
    return new QName(Namespaces.BEANS.xmlNamespace(), Namespaces.BEANS.eipNamespace());
  }

  @Override
  protected NodeTransformer getNodeTransformer() {
    return nodeTransformer;
  }
}
