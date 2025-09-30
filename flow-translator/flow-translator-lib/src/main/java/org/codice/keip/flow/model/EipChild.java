package org.codice.keip.flow.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record EipChild(EipId eipId, Map<String, Object> attributes, List<EipChild> children) {
  @Override
  public Map<String, Object> attributes() {
    if (attributes == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(attributes);
  }

  @Override
  public List<EipChild> children() {
    if (children == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(children);
  }
}
