package org.codice.keip.flow.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// TODO: Look into generating this class from the JSON schema.
// Should match the EipFlow schema defined at:
// <project-root>/keip-canvas/schemas/model/json/eipFlow.schema.json
public record Flow(List<EipNode> nodes, List<FlowEdge> edges, Map<String, String> customEntities) {
  public Flow(List<EipNode> nodes, List<FlowEdge> edges) {
    this(nodes, edges, Collections.emptyMap());
  }

  public Map<String, String> customEntities() {
    if (customEntities == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(customEntities);
  }
}
