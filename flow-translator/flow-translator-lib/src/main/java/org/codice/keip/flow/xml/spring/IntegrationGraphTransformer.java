package org.codice.keip.flow.xml.spring;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import javax.xml.namespace.QName;
import org.codice.keip.flow.xml.GraphTransformer;
import org.codice.keip.flow.xml.NamespaceSpec;
import org.codice.keip.flow.xml.NodeTransformerFactory;

public final class IntegrationGraphTransformer extends GraphTransformer {

  private IntegrationGraphTransformer(
      Collection<NamespaceSpec> namespaceSpecs, NodeTransformerFactory factory) {
    super(factory, namespaceSpecs);
  }

  public static IntegrationGraphTransformer createDefaultInstance(
      Collection<NamespaceSpec> namespaceSpecs) {
    NodeTransformerFactory defaultFactory =
        new NodeTransformerFactory(new DefaultNodeTransformer(), new DefaultXmlTransformer());
    return new IntegrationGraphTransformer(namespaceSpecs, defaultFactory);
  }

  public static IntegrationGraphTransformer createInstance(
      Collection<NamespaceSpec> namespaceSpecs,
      Consumer<NodeTransformerFactory> factoryCustomizer) {
    NodeTransformerFactory factory =
        new NodeTransformerFactory(new DefaultNodeTransformer(), new DefaultXmlTransformer());
    factoryCustomizer.accept(factory);
    return new IntegrationGraphTransformer(namespaceSpecs, factory);
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
