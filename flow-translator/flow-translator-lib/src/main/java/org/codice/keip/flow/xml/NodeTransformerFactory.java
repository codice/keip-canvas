package org.codice.keip.flow.xml;

import org.codice.keip.flow.model.EipId;
import java.util.HashMap;
import java.util.Map;

public class NodeTransformerFactory {

  private final Map<EipId, NodeTransformer> transformerRegistry = new HashMap<>();

  private final NodeTransformer defaultTransformer;

  public NodeTransformerFactory(NodeTransformer defaultTransformer) {
    this.defaultTransformer = defaultTransformer;
  }

  public void register(EipId id, NodeTransformer transformer) {
    this.transformerRegistry.put(id, transformer);
  }

  public NodeTransformer getTransformer(EipId id) {
    return transformerRegistry.getOrDefault(id, defaultTransformer);
  }
}
