package org.codice.keip.flow.xml;

import java.util.HashMap;
import java.util.Map;
import org.codice.keip.flow.model.EipId;

// TODO: Rename
public class NodeTransformerFactory {

  private final Map<EipId, NodeTransformer> transformerRegistry = new HashMap<>();

  private final NodeTransformer defaultNodeTransformer;

  private final XmlTransformer defaultXmlTransformer;

  public NodeTransformerFactory(
      NodeTransformer defaultNodeTransformer, XmlTransformer defaultXmlTransformer) {
    this.defaultNodeTransformer = defaultNodeTransformer;
    this.defaultXmlTransformer = defaultXmlTransformer;
  }

  public void registerNodeTransformer(EipId id, NodeTransformer transformer) {
    this.transformerRegistry.put(id, transformer);
  }

  public NodeTransformer getNodeTransformer(EipId id) {
    return transformerRegistry.getOrDefault(id, defaultNodeTransformer);
  }

  public XmlTransformer getXmlTransformer() {
    return defaultXmlTransformer;
  }
}
