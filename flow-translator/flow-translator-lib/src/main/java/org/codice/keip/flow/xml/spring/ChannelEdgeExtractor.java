package org.codice.keip.flow.xml.spring;

import static org.codice.keip.flow.xml.spring.AttributeNames.CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.DISCARD_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.INPUT_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.OUTPUT_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.REPLY_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.REQUEST_CHANNEL;
import static org.codice.keip.flow.xml.spring.ComponentIdentifiers.DIRECT_CHANNEL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.codice.keip.flow.graph.GuavaGraph;
import org.codice.keip.flow.model.ConnectionType;
import org.codice.keip.flow.model.EdgeProps;
import org.codice.keip.flow.model.EdgeProps.EdgeType;
import org.codice.keip.flow.model.EipNode;

// TODO: Javadoc
// Assumption: EipNodes must have a unique id
public class ChannelEdgeExtractor {

  private final Collection<EipNode> nodes;

  private final Map<String, ChannelConnections> channelConnections;

  private final Map<String, EipNode> channelNodes;

  private final GuavaGraph.Builder graphBuilder;

  private static final Set<String> CHANNEL_ATTRIBUTES =
      Set.of(
          CHANNEL, INPUT_CHANNEL, OUTPUT_CHANNEL, DISCARD_CHANNEL, REQUEST_CHANNEL, REPLY_CHANNEL);

  public ChannelEdgeExtractor(Collection<EipNode> nodes) {
    this.nodes = nodes;
    this.channelConnections = new HashMap<>();
    this.channelNodes = new HashMap<>();
    this.graphBuilder = GuavaGraph.newBuilder();
  }

  // TODO: Handle multiple input/outputs to channel
  // TODO: Test out-of-order XML
  public GuavaGraph buildGraph() {
    for (EipNode node : nodes) {
      if (isDirectChannel(node)) {
        channelConnections.putIfAbsent(node.id(), new ChannelConnections());
        channelNodes.put(node.id(), node);
        continue;
      }

      List<String> channelAttributes = collectChannelAttributes(node);
      channelAttributes.forEach(node.mutableAttributes()::remove);
      graphBuilder.addNode(node);
    }

    channelConnections.forEach(this::addChannelAsGraphEdge);
    return graphBuilder.build();
  }

  private List<String> collectChannelAttributes(EipNode node) {
    List<String> channelAttributes = new ArrayList<>();
    for (Entry<String, Object> attr : node.attributes().entrySet()) {
      if (CHANNEL_ATTRIBUTES.contains(attr.getKey())) {
        channelAttributes.add(attr.getKey());
        channelConnections.putIfAbsent(attr.getValue().toString(), new ChannelConnections());
        addChannelConnection(
            attr,
            node.id(),
            node.connectionType(),
            channelConnections.get(attr.getValue().toString()));
      }
    }
    return channelAttributes;
  }

  private void addChannelAsGraphEdge(String channelId, ChannelConnections connections) {
    if (connections.incoming.isEmpty() || connections.outgoing.isEmpty()) {
      // TODO: handle disconnected edge
      return;
    }

    // if channel has multiple inputs or outputs -> include as a standalone node in the graph
    if (connections.incoming.size() > 1 || connections.outgoing.size() > 1) {
      graphBuilder.addNode(channelNodes.get(channelId));
      connections.incoming().forEach(conn -> addGraphEdge("", conn, new Connection(channelId)));
      connections.outgoing().forEach(conn -> addGraphEdge("", new Connection(channelId), conn));
    }

    addGraphEdge(channelId, connections.incoming().getFirst(), connections.outgoing().getLast());
  }

  private void addGraphEdge(String channelId, Connection incoming, Connection outgoing) {
    String edgeId = getEdgeId(channelId, incoming.node(), outgoing.node());
    EdgeProps edgeProps = new EdgeProps(edgeId, incoming.type());
    graphBuilder.putEdgeValue(incoming.node(), outgoing.node(), edgeProps);
  }

  private void addChannelConnection(
      Entry<String, Object> attr,
      String nodeId,
      ConnectionType connectionType,
      ChannelConnections connections) {

    switch (attr.getKey()) {
      case DISCARD_CHANNEL, OUTPUT_CHANNEL, REPLY_CHANNEL ->
          addIncomingConnection(attr, nodeId, connections); // flowing into channel
      case INPUT_CHANNEL, REQUEST_CHANNEL ->
          addOutgoingConnection(nodeId, connections); // flowing out of channel
      case CHANNEL -> disambiguateChannelConnection(nodeId, connections, connectionType);
      default ->
          throw new RuntimeException(
              String.format("unknown channel connection attribute: '%s'", attr.getKey()));
    }
  }

  private void addIncomingConnection(
      Entry<String, Object> attr, String nodeId, ChannelConnections connections) {
    EdgeType type = attr.getKey().equals(DISCARD_CHANNEL) ? EdgeType.DISCARD : EdgeType.DEFAULT;
    connections.addIncoming(new Connection(nodeId, type));
  }

  private void addOutgoingConnection(String nodeId, ChannelConnections connections) {
    connections.addOutgoing(new Connection(nodeId));
  }

  private void disambiguateChannelConnection(
      String nodeId, ChannelConnections connections, ConnectionType connectionType) {
    boolean isSourceNode = ConnectionType.SOURCE.equals(connectionType);
    if (isSourceNode) {
      connections.addIncoming(new Connection(nodeId));
    } else {
      connections.addOutgoing(new Connection(nodeId));
    }
  }

  private boolean isDirectChannel(EipNode node) {
    return node.eipId().equals(DIRECT_CHANNEL) && node.children().isEmpty();
  }

  private record Connection(String node, EdgeType type) {
    public Connection(String node) {
      this(node, EdgeType.DEFAULT);
    }
  }

  private record ChannelConnections(List<Connection> incoming, List<Connection> outgoing) {
    private ChannelConnections() {
      this(new ArrayList<>(), new ArrayList<>());
    }

    private void addIncoming(Connection connection) {
      incoming.add(connection);
    }

    private void addOutgoing(Connection connection) {
      outgoing.add(connection);
    }
  }

  private String getEdgeId(String channelId, String sourceId, String targetId) {
    if (channelId != null && !channelId.isBlank()) {
      return channelId;
    }
    return sourceId + "-" + targetId;
  }
}
