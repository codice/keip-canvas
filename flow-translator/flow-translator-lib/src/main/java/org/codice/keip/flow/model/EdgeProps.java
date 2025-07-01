package org.codice.keip.flow.model;

public record EdgeProps(String id, EdgeType type) {
  public enum EdgeType {
    DEFAULT,
    DISCARD;
  }

  public EdgeProps(String id) {
    this(id, EdgeType.DEFAULT);
  }

  public static EdgeProps from(FlowEdge edge) {
    return new EdgeProps(edge.id(), edge.type());
  }
}
