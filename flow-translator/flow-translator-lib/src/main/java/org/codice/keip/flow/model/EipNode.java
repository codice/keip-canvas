package org.codice.keip.flow.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a specific instantiation of an EIP component in the graph (e.g. a message filter or
 * router). Ids must be unique across the flow graph.
 *
 * <p>Equality and hash code functions are defined using the 'id' field only.
 */
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

  public EipNode withAttributes(Map<String, Object> attrs) {
    return new EipNode(id, eipId, label, description, role, connectionType, attrs, children);
  }

  @Override
  public List<EipChild> children() {
    if (children == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(children);
  }

  public EipNode withChildren(List<EipChild> childList) {
    return new EipNode(id, eipId, label, description, role, connectionType, attributes, childList);
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
