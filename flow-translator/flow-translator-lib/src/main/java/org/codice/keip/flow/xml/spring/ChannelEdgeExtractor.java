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
import java.util.Set;
import org.codice.keip.flow.graph.GuavaGraph;
import org.codice.keip.flow.graph.GuavaGraph.Builder;
import org.codice.keip.flow.model.ConnectionType;
import org.codice.keip.flow.model.EdgeProps.EdgeType;
import org.codice.keip.flow.model.EipNode;

// TODO: Javadoc
public class ChannelEdgeExtractor {

  private static final Set<String> CHANNEL_ATTRIBUTES =
      Set.of(
          CHANNEL, INPUT_CHANNEL, OUTPUT_CHANNEL, DISCARD_CHANNEL, REQUEST_CHANNEL, REPLY_CHANNEL);

  // TODO: Handle multiple input/outputs to channel
  // TODO: Test out-of-order XML
  // TODO: Make static?
  public GuavaGraph buildGraph(Collection<EipNode> nodes) {
    Builder graphBuilder = GuavaGraph.newBuilder();
    Map<String, ChannelConnections> channelConnections = new HashMap<>();
    for (EipNode node : nodes) {
      if (isDirectChannel(node)) {
        channelConnections.putIfAbsent(node.id(), new ChannelConnections());
        continue;
      }

      List<String> channelAttributes = new ArrayList<>();
      node.attributes()
          .forEach(
              (name, value) -> {
                if (CHANNEL_ATTRIBUTES.contains(name)) {
                  channelAttributes.add(name);
                  channelConnections.putIfAbsent(value.toString(), new ChannelConnections());
                  addChannelConnection(
                      name,
                      value.toString(),
                      node.connectionType(),
                      channelConnections.get(value.toString()));
                }
              });

      channelAttributes.forEach(node.mutableAttributes()::remove);
      graphBuilder.addNode(node);
    }
    return graphBuilder.build();
  }

  private void addChannelConnection(
      String attrName,
      String attrVal,
      ConnectionType connectionType,
      ChannelConnections connections) {

    switch (attrName) {
      case DISCARD_CHANNEL, OUTPUT_CHANNEL, REPLY_CHANNEL ->
          // flowing into channel
          addIncomingConnection(attrName, attrVal, connections);
      case INPUT_CHANNEL, REQUEST_CHANNEL ->
          // flowing out of channel
          addOutgoingConnection(attrVal, connections);
      case CHANNEL -> disambiguateChannelConnection(attrVal, connections, connectionType);
      default ->
          throw new RuntimeException(
              String.format("unknown channel connection attribute: '%s'", attrName));
    }
  }

  private void addIncomingConnection(
      String attrName, String attrVal, ChannelConnections connections) {
    EdgeType type = attrName.equals(DISCARD_CHANNEL) ? EdgeType.DISCARD : EdgeType.DEFAULT;
    connections.addIncoming(new Connection(attrVal, type));
  }

  private void addOutgoingConnection(String attrVal, ChannelConnections connections) {
    connections.addOutgoing(new Connection(attrVal, EdgeType.DEFAULT));
  }

  private void disambiguateChannelConnection(
      String attrVal, ChannelConnections connections, ConnectionType connectionType) {
    boolean isSourceNode = ConnectionType.SOURCE.equals(connectionType);
    if (isSourceNode) {
      connections.addIncoming(new Connection(attrVal, EdgeType.DEFAULT));
    } else {
      connections.addOutgoing(new Connection(attrVal, EdgeType.DEFAULT));
    }
  }

  private boolean isDirectChannel(EipNode node) {
    return node.eipId().equals(DIRECT_CHANNEL) && node.children().isEmpty();
  }

  private record Connection(String node, EdgeType type) {}

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
}
