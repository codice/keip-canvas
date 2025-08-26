package org.codice.keip.flow.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Serves as an intermediate between the {@link Flow} model and backend representations such as XML.
 * The use of {@code EipGraph} makes it easier to support multiple backends consistently, since all
 * conversions pass through the same intermediate graph abstraction.
 */
public interface EipGraph {
  Stream<EipNode> traverse();

  Set<EipNode> predecessors(EipNode node);

  Set<EipNode> successors(EipNode node);

  Optional<EdgeProps> getEdgeProps(EipNode source, EipNode target);

  Flow toFlow();
}
