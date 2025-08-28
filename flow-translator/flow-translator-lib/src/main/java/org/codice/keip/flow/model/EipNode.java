package org.codice.keip.flow.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Represents a specific instantiation of an EIP component in the graph (e.g. a message filter or
// router). Ids must be unique across the flow graph.

// TODO: Should FlowType and Role be stored elsewhere (e.g. a registry) and looked up with eipId
public record EipNode(
    String id,
    EipId eipId,
    String label,
    String description,
    Role role,
    ConnectionType connectionType,
    Map<String, Object> attributes,
    List<EipChild> children) {

  @Override
  public Map<String, Object> attributes() {
    if (attributes == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(attributes);
  }

  // TODO: Use builder instead?
  public Map<String, Object> mutableAttributes() {
    if (attributes == null) {
      return new LinkedHashMap<>();
    }
    return attributes;
  }

  @Override
  public List<EipChild> children() {
    if (children == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(children);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EipNode eipNode)) {
      return false;
    }
    return Objects.equals(id, eipNode.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return id;
  }
}
