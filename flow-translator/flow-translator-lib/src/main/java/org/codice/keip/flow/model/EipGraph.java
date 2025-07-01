package org.codice.keip.flow.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface EipGraph {
  Stream<EipNode> traverse();

  Set<EipNode> predecessors(EipNode node);

  Set<EipNode> successors(EipNode node);

  Optional<EdgeProps> getEdgeProps(EipNode source, EipNode target);
}
