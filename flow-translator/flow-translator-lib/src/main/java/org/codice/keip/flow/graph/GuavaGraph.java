package org.codice.keip.flow.graph;

import com.google.common.collect.Streams;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.Traverser;
import com.google.common.graph.ValueGraphBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.codice.keip.flow.model.EdgeProps;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.model.Flow;
import org.codice.keip.flow.model.FlowEdge;

// Guava Graph API is still marked as beta
@SuppressWarnings("UnstableApiUsage")
public class GuavaGraph implements EipGraph {

  private final ImmutableValueGraph<EipNode, EdgeProps> graph;

  private GuavaGraph(ImmutableValueGraph<EipNode, EdgeProps> graph) {
    this.graph = graph;
  }

  public static GuavaGraph from(Flow flow) {
    Builder builder = newBuilder();
    flow.nodes().forEach(builder::addNode);
    flow.edges()
        .forEach(edge -> builder.putEdgeValue(edge.source(), edge.target(), EdgeProps.from(edge)));
    return builder.build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public Stream<EipNode> traverse() {
    Stream<EipNode> roots = findRoots();
    Iterable<EipNode> nodes = Traverser.forGraph(this.graph).depthFirstPreOrder(roots.toList());
    return Streams.stream(nodes);
  }

  @Override
  public Set<EipNode> predecessors(EipNode node) {
    return this.graph.predecessors(node);
  }

  @Override
  public Set<EipNode> successors(EipNode node) {
    return this.graph.successors(node);
  }

  @Override
  public Optional<EdgeProps> getEdgeProps(EipNode source, EipNode target) {
    return this.graph.edgeValue(source, target);
  }

  @Override
  public Flow toFlow() {
    List<EipNode> nodes = new ArrayList<>();
    List<FlowEdge> edges = new ArrayList<>();

    traverse()
        .forEach(
            node -> {
              nodes.add(node);
              successors(node).stream()
                  .map(target -> graphToFlowEdge(node, target))
                  .forEach(edges::add);
            });

    return new Flow(nodes, edges);
  }

  private FlowEdge graphToFlowEdge(EipNode source, EipNode target) {
    EdgeProps ep = getEdgeProps(source, target).orElseThrow();
    return new FlowEdge(ep.id(), source.id(), target.id(), ep.type());
  }

  /** Finds the nodes in the graph that have no incoming edges (indicating the start of a flow). */
  private Stream<EipNode> findRoots() {
    Stream<EipNode> roots = graph.nodes().stream().filter(node -> graph.inDegree(node) == 0);
    if (Graphs.hasCycle(graph.asGraph())) {
      // traverse acyclic components first
      return Stream.concat(roots, graph.nodes().stream());
    }
    return roots;
  }

  public static class Builder {
    private final com.google.common.graph.ImmutableValueGraph.Builder<EipNode, EdgeProps> builder =
        ValueGraphBuilder.directed().immutable();

    private final Map<String, EipNode> visitedNodes = new HashMap<>();

    public Builder addNode(EipNode node) {
      if (visitedNodes.containsKey(node.id())) {
        throw new IllegalArgumentException(String.format("Duplicate node id: %s", node.id()));
      }
      visitedNodes.put(node.id(), node);
      builder.addNode(node);
      return this;
    }

    public Builder putEdgeValue(String sourceId, String targetId, EdgeProps value) {
      EipNode sourceNode = visitedNodes.get(sourceId);
      EipNode targetNode = visitedNodes.get(targetId);
      if (sourceNode == null || targetNode == null) {
        throw new IllegalArgumentException(
            String.format("A graph edge is detached: (%s, %s)", sourceId, targetId));
      }
      builder.putEdgeValue(sourceNode, targetNode, value);
      return this;
    }

    public GuavaGraph build() {
      return new GuavaGraph(builder.build());
    }
  }
}
