package org.codice.keip.flow.xml.spring;

import static org.codice.keip.flow.xml.spring.AttributeNames.CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.DEFAULT_OUTPUT_CHANNEL_NAME;
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
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.graph.GuavaGraph;
import org.codice.keip.flow.model.ConnectionType;
import org.codice.keip.flow.model.EdgeProps;
import org.codice.keip.flow.model.EdgeProps.EdgeType;
import org.codice.keip.flow.model.EipNode;

/**
 * Converts direct channel nodes that have exactly one input and one output connection into a
 * corresponding edge in the graph.
 *
 * <p>NOTE: references to incoming and outgoing connections in this class are from the perspective
 * of the channel. In other words, incoming connection -> flowing into channel, outgoing connection
 * -> flowing out of channel.
 */
class ChannelEdgeBuilder {

  private static final Set<String> CHANNEL_ATTRIBUTES =
      Set.of(
          CHANNEL,
          INPUT_CHANNEL,
          OUTPUT_CHANNEL,
          DISCARD_CHANNEL,
          REQUEST_CHANNEL,
          REPLY_CHANNEL,
          DEFAULT_OUTPUT_CHANNEL_NAME);

  private static final Set<String> CHANNEL_ROUTING_CHILDREN = Set.of("mapping", "recipient");

  private final Collection<EipNode> nodes;

  private final Map<String, ChannelConnections> channelConnections;

  private final Map<String, EipNode> channelNodes;

  private final GuavaGraph.Builder graphBuilder;

  ChannelEdgeBuilder(Collection<EipNode> nodes) {
    this.nodes = nodes;
    this.channelConnections = new HashMap<>();
    this.channelNodes = new HashMap<>();
    this.graphBuilder = GuavaGraph.newBuilder();
  }

  // TODO: Support partial errors by returning a "TranslationResult" record
  GuavaGraph buildGraph() throws TransformerException {
    for (EipNode node : nodes) {
      if (isDirectChannel(node)) {
        channelConnections.putIfAbsent(node.id(), new ChannelConnections());
        channelNodes.put(node.id(), node);
        continue;
      }

      List<String> channelAttributes = processChannelAttributes(node);
      processContentBasedRouters(node);
      channelAttributes.forEach(node.mutableAttributes()::remove);
      graphBuilder.addNode(node);
    }

    for (Entry<String, ChannelConnections> entry : channelConnections.entrySet()) {
      addChannelAsGraphEdge(entry.getKey(), entry.getValue());
    }

    return graphBuilder.build();
  }

  private List<String> processChannelAttributes(EipNode node) {
    List<String> channelAttributes = new ArrayList<>();
    for (Entry<String, Object> attr : node.attributes().entrySet()) {
      if (CHANNEL_ATTRIBUTES.contains(attr.getKey())) {
        channelAttributes.add(attr.getKey());
        ChannelConnections connections =
            channelConnections.computeIfAbsent(
                attr.getValue().toString(), k -> new ChannelConnections());
        addChannelConnection(attr, node.id(), node.connectionType(), connections);
      }
    }
    return channelAttributes;
  }

  private void processContentBasedRouters(EipNode node) throws TransformerException {
    if (!node.connectionType().equals(ConnectionType.CONTENT_BASED_ROUTER)) {
      return;
    }

    for (var child : node.children()) {
      if (CHANNEL_ROUTING_CHILDREN.contains(child.name())) {
        Object channel = child.attributes().get(CHANNEL);
        if (channel == null) {
          throw new TransformerException(
              String.format(
                  "Unable to process node (%s): 'mapping' or 'recipient' children must have a 'channel' attribute",
                  node.id()));
        }

        channelConnections
            .computeIfAbsent(channel.toString(), k -> new ChannelConnections())
            .addIncoming(new Connection(node.id()));
      }
    }
  }

  private void addChannelAsGraphEdge(String channelId, ChannelConnections connections)
      throws TransformerException {
    if (connections.incoming.isEmpty() || connections.outgoing.isEmpty()) {
      throw new TransformerException(String.format("disconnected channel: '%s'", channelId));
    }

    // if channel has multiple inputs or outputs -> include as a standalone node in the graph
    if (connections.incoming.size() > 1 || connections.outgoing.size() > 1) {
      graphBuilder.addNode(channelNodes.get(channelId));
      connections.incoming().forEach(conn -> addGraphEdge("", conn, new Connection(channelId)));
      connections.outgoing().forEach(conn -> addGraphEdge("", new Connection(channelId), conn));
      return;
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
      case OUTPUT_CHANNEL, DEFAULT_OUTPUT_CHANNEL_NAME ->
          addIncomingConnection(nodeId, connections);
      case DISCARD_CHANNEL -> connections.addIncoming(new Connection(nodeId, EdgeType.DISCARD));
      case INPUT_CHANNEL -> addOutgoingConnection(nodeId, connections);
      case REQUEST_CHANNEL -> disambiguateRequestConnection(nodeId, connectionType, connections);
      case REPLY_CHANNEL -> disambiguateReplyConnection(nodeId, connectionType, connections);
      case CHANNEL -> disambiguateChannelConnection(nodeId, connectionType, connections);
      default ->
          throw new RuntimeException(
              String.format("unknown channel connection attribute: '%s'", attr.getKey()));
    }
  }

  // flowing into channel
  private void addIncomingConnection(String nodeId, ChannelConnections connections) {
    connections.addIncoming(new Connection(nodeId));
  }

  // flowing out of channel
  private void addOutgoingConnection(String nodeId, ChannelConnections connections) {
    connections.addOutgoing(new Connection(nodeId));
  }

  private void disambiguateChannelConnection(
      String nodeId, ConnectionType connectionType, ChannelConnections connections) {
    boolean isSourceNode = ConnectionType.SOURCE.equals(connectionType);
    if (isSourceNode) {
      connections.addIncoming(new Connection(nodeId));
    } else {
      connections.addOutgoing(new Connection(nodeId));
    }
  }

  private void disambiguateRequestConnection(
      String nodeId, ConnectionType connectionType, ChannelConnections connections) {
    if (ConnectionType.REQUEST_REPLY.equals(connectionType)) {
      addOutgoingConnection(nodeId, connections);
    } else if (ConnectionType.INBOUND_REQUEST_REPLY.equals(connectionType)) {
      addIncomingConnection(nodeId, connections);
    }
  }

  private void disambiguateReplyConnection(
      String nodeId, ConnectionType connectionType, ChannelConnections connections) {
    if (ConnectionType.REQUEST_REPLY.equals(connectionType)) {
      addIncomingConnection(nodeId, connections);
    } else if (ConnectionType.INBOUND_REQUEST_REPLY.equals(connectionType)) {
      addOutgoingConnection(nodeId, connections);
    }
  }

  private boolean isDirectChannel(EipNode node) {
    return node.eipId().equals(DIRECT_CHANNEL) && node.children().isEmpty();
  }

  private record Connection(String node, EdgeType type) {
    private Connection(String node) {
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
    return String.format("ch-%s-%s", sourceId, targetId);
  }
}
