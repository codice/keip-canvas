package org.codice.keip.flow.xml.spring;

import org.codice.keip.flow.xml.GraphTransformer;
import org.codice.keip.flow.xml.NamespaceSpec;
import org.codice.keip.flow.xml.NodeTransformerFactory;
import java.util.Collection;
import java.util.Set;
import javax.xml.namespace.QName;

public final class IntegrationGraphTransformer extends GraphTransformer {

  public IntegrationGraphTransformer(Collection<NamespaceSpec> namespaceSpecs) {
    super(new NodeTransformerFactory(new DefaultNodeTransformer()), namespaceSpecs);
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
}
