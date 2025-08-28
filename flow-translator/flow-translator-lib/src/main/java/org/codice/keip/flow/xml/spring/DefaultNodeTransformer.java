package org.codice.keip.flow.xml.spring;

import static org.codice.keip.flow.xml.spring.AttributeNames.CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.DISCARD_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.ID;
import static org.codice.keip.flow.xml.spring.AttributeNames.INPUT_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.OUTPUT_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.REPLY_CHANNEL;
import static org.codice.keip.flow.xml.spring.AttributeNames.REQUEST_CHANNEL;
import static org.codice.keip.flow.xml.spring.ComponentIdentifiers.DIRECT_CHANNEL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codice.keip.flow.model.EdgeProps;
import org.codice.keip.flow.model.EdgeProps.EdgeType;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.model.Role;
import org.codice.keip.flow.xml.NodeTransformer;
import org.codice.keip.flow.xml.XmlElement;

/**
 * A default implementation for generating {@link XmlElement}s from an {@link EipNode}. The
 * transformer should be suitable for components with simple input/output schemes, and is also
 * responsible for creating the intermediate channels connecting the XmlElements.
 */
public class DefaultNodeTransformer implements NodeTransformer {

  @Override
  public List<XmlElement> apply(EipNode node, EipGraph graph) {
    XmlElement element = BaseNodeTransformation.toXmlElement(node);
    DefaultTransformation transformation = new DefaultTransformation(node, graph);
    element = transformation.updateAttributes(element);

    List<XmlElement> elements = new ArrayList<>();
    elements.add(element);
    elements.addAll(transformation.createDirectChannels());
    return elements;
  }

  record DefaultTransformation(EipNode node, EipGraph graph) {

    private XmlElement updateAttributes(XmlElement element) {
      Map<String, Object> updatedAttrs = new LinkedHashMap<>();
      updatedAttrs.put(ID, this.node.id());
      updatedAttrs.putAll(createChannelAttributes());
      updatedAttrs.putAll(element.attributes());
      return new XmlElement(
          element.prefix(), element.localName(), updatedAttrs, element.children());
    }

    /**
     * Creates the intermediate channels between the node and its direct successors, provided the
     * current node and the successors are not channel nodes themselves.
     *
     * @return A list of direct channels
     */
    List<XmlElement> createDirectChannels() {
      if (Role.CHANNEL.equals(this.node.role())) {
        return Collections.emptyList();
      }

      // Create downstream channels
      return this.graph.successors(this.node).stream()
          .filter(s -> !Role.CHANNEL.equals(s.role()))
          .map(s -> getChannelId(this.node, s))
          .map(
              channelId ->
                  new XmlElement(
                      DIRECT_CHANNEL.namespace(),
                      DIRECT_CHANNEL.name(),
                      Map.of(ID, channelId),
                      Collections.emptyList()))
          .toList();
    }

    /**
     * Adds attributes for targeting the intermediate channels connecting each node with its
     * immediate predecessors and successors.
     */
    Map<String, Object> createChannelAttributes() {
      if (Role.CHANNEL.equals(this.node.role())) {
        return Collections.emptyMap();
      }

      Set<EipNode> predecessors = this.graph.predecessors(this.node);
      Set<EipNode> successors = this.graph.successors(this.node);

      Map<String, Object> attributes = new LinkedHashMap<>();
      return switch (this.node.connectionType()) {
        case CONTENT_BASED_ROUTER -> handleRouter(attributes, predecessors);
        case INBOUND_REQUEST_REPLY ->
            handleInboundRequestReply(attributes, predecessors, successors);
        case PASSTHRU -> handlePassthru(attributes, predecessors, successors);
        case REQUEST_REPLY -> handleRequestReply(attributes, predecessors, successors);
        case SINK -> handleSink(attributes, predecessors, successors);
        case SOURCE -> handleSource(attributes, predecessors, successors);
        case TEE -> handleTee(attributes, predecessors, successors);
      };
    }

    private Map<String, Object> handleRouter(
        Map<String, Object> attributes, Set<EipNode> predecessors) {
      if (predecessors.size() > 1) {
        throw new IllegalArgumentException(
            "'ContentBasedRouter' nodes can have at most one incoming edge");
      }
      predecessors.stream()
          .findFirst()
          .ifPresent(p -> attributes.put(INPUT_CHANNEL, getChannelId(p, this.node)));
      return attributes;
    }

    private Map<String, Object> handleInboundRequestReply(
        Map<String, Object> attributes, Set<EipNode> predecessors, Set<EipNode> successors) {
      if (predecessors.size() > 1 || successors.size() > 1) {
        throw new IllegalArgumentException(
            "'RequestReply' nodes can have at most one incoming and one outgoing edge");
      }
      predecessors.stream()
          .findFirst()
          .ifPresent(p -> attributes.put(REPLY_CHANNEL, getChannelId(p, this.node)));
      successors.stream()
          .findFirst()
          .ifPresent(s -> attributes.put(REQUEST_CHANNEL, getChannelId(this.node, s)));
      return attributes;
    }

    private Map<String, Object> handlePassthru(
        Map<String, Object> attributes, Set<EipNode> predecessors, Set<EipNode> successors) {
      if (predecessors.size() > 1 || successors.size() > 1) {
        throw new IllegalArgumentException(
            "'Passthru' nodes can have at most one incoming and one outgoing edge");
      }
      predecessors.stream()
          .findFirst()
          .ifPresent(p -> attributes.put(INPUT_CHANNEL, getChannelId(p, this.node)));
      successors.stream()
          .findFirst()
          .ifPresent(s -> attributes.put(OUTPUT_CHANNEL, getChannelId(this.node, s)));
      return attributes;
    }

    private Map<String, Object> handleRequestReply(
        Map<String, Object> attributes, Set<EipNode> predecessors, Set<EipNode> successors) {
      if (predecessors.size() > 1 || successors.size() > 1) {
        throw new IllegalArgumentException(
            "'RequestReply' nodes can have at most one incoming and one outgoing edge");
      }
      predecessors.stream()
          .findFirst()
          .ifPresent(p -> attributes.put(REQUEST_CHANNEL, getChannelId(p, this.node)));
      successors.stream()
          .findFirst()
          .ifPresent(s -> attributes.put(REPLY_CHANNEL, getChannelId(this.node, s)));
      return attributes;
    }

    private Map<String, Object> handleSink(
        Map<String, Object> attributes, Set<EipNode> predecessors, Set<EipNode> successors) {
      if (predecessors.size() > 1 || !successors.isEmpty()) {
        throw new IllegalArgumentException("'Sink' nodes can have at most one incoming edge");
      }
      predecessors.stream()
          .findFirst()
          .ifPresent(p -> attributes.put(CHANNEL, getChannelId(p, this.node)));
      return attributes;
    }

    private Map<String, Object> handleSource(
        Map<String, Object> attributes, Set<EipNode> predecessors, Set<EipNode> successors) {
      if (!predecessors.isEmpty() || successors.size() > 1) {
        throw new IllegalArgumentException("'Source' nodes can have at most one outgoing edge");
      }
      successors.stream()
          .findFirst()
          .ifPresent(s -> attributes.put(CHANNEL, getChannelId(this.node, s)));
      return attributes;
    }

    // Assumes at most two successors - an output-channel and an optional discard-channel
    private Map<String, Object> handleTee(
        Map<String, Object> attributes, Set<EipNode> predecessors, Set<EipNode> successors) {
      RuntimeException invalidError =
          new IllegalArgumentException(
              "'Tee' nodes can have at most one incoming edge, and two outgoing edges (one output and one discard edge)");
      if (predecessors.size() > 1 || successors.size() > 2) {
        throw invalidError;
      }

      predecessors.stream()
          .findFirst()
          .ifPresent(p -> attributes.put(INPUT_CHANNEL, getChannelId(p, this.node)));

      for (EipNode successor : this.graph.successors(this.node)) {
        EdgeType type =
            this.graph
                .getEdgeProps(this.node, successor)
                .map(EdgeProps::type)
                .orElse(EdgeType.DEFAULT);
        if (type.equals(EdgeType.DISCARD)) {
          if (attributes.containsKey(DISCARD_CHANNEL)) { // only one discard channel is allowed
            throw invalidError;
          }
          attributes.put(DISCARD_CHANNEL, getChannelId(this.node, successor));
        } else {
          if (attributes.containsKey(OUTPUT_CHANNEL)) { // only one output channel is allowed
            throw invalidError;
          }
          attributes.put(OUTPUT_CHANNEL, getChannelId(this.node, successor));
        }
      }

      return attributes;
    }

    // TODO: Might be used by other transformers. Consider extracting.
    private String getChannelId(EipNode source, EipNode target) {
      if (Role.CHANNEL.equals(source.role())) {
        return source.id();
      } else if (Role.CHANNEL.equals(target.role())) {
        return target.id();
      } else {
        return this.graph
            .getEdgeProps(source, target)
            .map(EdgeProps::id)
            .orElse(source.id() + "-" + target.id());
      }
    }
  }
}
