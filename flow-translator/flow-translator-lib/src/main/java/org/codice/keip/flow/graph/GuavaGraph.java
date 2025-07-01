package org.codice.keip.flow.graph;

import com.google.common.collect.Streams;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ImmutableValueGraph.Builder;
import com.google.common.graph.Traverser;
import com.google.common.graph.ValueGraphBuilder;
import org.codice.keip.flow.model.Flow;
import org.codice.keip.flow.model.FlowEdge;
import org.codice.keip.flow.model.EdgeProps;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

// Guava Graph API is still marked as beta
@SuppressWarnings("UnstableApiUsage")
public class GuavaGraph implements EipGraph {

  private final ImmutableValueGraph<EipNode, EdgeProps> graph;

  private GuavaGraph(ImmutableValueGraph<EipNode, EdgeProps> graph) {
    if (Graphs.hasCycle(graph.asGraph())) {
      throw new IllegalArgumentException("Graphs with cycles are not allowed");
    }
    this.graph = graph;
  }

  public static GuavaGraph from(Flow flow) {
    Map<String, EipNode> visitedNodes = new HashMap<>();
    Builder<EipNode, EdgeProps> builder = ValueGraphBuilder.directed().immutable();
    addNodes(flow.nodes(), builder, visitedNodes);
    addEdges(flow.edges(), builder, visitedNodes);
    return new GuavaGraph(builder.build());
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

  /** Finds the nodes in the graph that have no incoming edges (indicating the start of a flow). */
  private Stream<EipNode> findRoots() {
    return graph.nodes().stream().filter(node -> graph.inDegree(node) == 0);
  }

  private static void addNodes(
      List<EipNode> nodes,
      ImmutableValueGraph.Builder<EipNode, EdgeProps> builder,
      Map<String, EipNode> visitedNodes) {
    for (EipNode node : nodes) {
      if (visitedNodes.containsKey(node.id())) {
        throw new IllegalArgumentException(String.format("Duplicate node id: %s", node.id()));
      }
      visitedNodes.put(node.id(), node);
      builder.addNode(node);
    }
  }

  private static void addEdges(
      List<FlowEdge> edges,
      Builder<EipNode, EdgeProps> builder,
      Map<String, EipNode> visitedNodes) {
    for (FlowEdge edge : edges) {
      EipNode source = visitedNodes.get(edge.source());
      EipNode target = visitedNodes.get(edge.target());
      if (source == null || target == null) {
        throw new IllegalArgumentException(String.format("A graph edge is detached: %s", edge));
      }
      builder.putEdgeValue(source, target, EdgeProps.from(edge));
    }
  }
}
