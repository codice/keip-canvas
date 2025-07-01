package org.codice.keip.flow.model;

import java.util.List;

// TODO: Look into generating this class from the JSON schema.
// Should match the EipFlow schema defined at:
// <project-root>/keip-canvas/schemas/model/json/eipFlow.schema.json
public record Flow(List<EipNode> nodes, List<FlowEdge> edges) {}
