package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EdgeProps
import org.codice.keip.flow.model.EipChild
import org.codice.keip.flow.model.EipGraph
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Role
import spock.lang.Specification

import static org.codice.keip.flow.xml.spring.AttributeNames.CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.DISCARD_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.ID
import static org.codice.keip.flow.xml.spring.AttributeNames.INPUT_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.OUTPUT_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.REPLY_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.REQUEST_CHANNEL
import static org.codice.keip.flow.xml.spring.DefaultNodeTransformer.DIRECT_CHANNEL

class DefaultNodeTransformerTest extends Specification {

    static final TEST_NS = "test-ns"

    EipGraph graph = Stub()

    DefaultNodeTransformer transformer = new DefaultNodeTransformer()

    EipNode testNode = createNodeStub("default-test-id")

    def "multi-input 'router' node fails validation"() {
        given:
        graph.predecessors(testNode) >> [createNodeStub("n1"), createNodeStub("n2")]
        graph.successors(testNode) >> []

        when:
        transformer.apply(testNode, graph)

        then:
        testNode.connectionType() >> ConnectionType.CONTENT_BASED_ROUTER
        thrown(IllegalArgumentException)
    }

    def "multi-input/multi-output 'passthru' node fails validation"(Set<EipNode> predecessors, Set<EipNode> successors) {
        given:
        graph.predecessors(testNode) >> predecessors
        graph.successors(testNode) >> successors

        when:
        transformer.apply(testNode, graph)

        then:
        testNode.connectionType() >> ConnectionType.PASSTHRU
        thrown(IllegalArgumentException)

        where:
        predecessors                                 | successors
        [createNodeStub("n1"), createNodeStub("n2")] | []
        []                                           | [createNodeStub("n1"), createNodeStub("n2")]
    }

    def "multi-input/multi-output 'request_reply' node fails validation"(Set<EipNode> predecessors, Set<EipNode> successors) {
        given:
        graph.predecessors(testNode) >> predecessors
        graph.successors(testNode) >> successors

        when:
        transformer.apply(testNode, graph)

        then:
        testNode.connectionType() >> ConnectionType.REQUEST_REPLY
        thrown(IllegalArgumentException)

        where:
        predecessors                                 | successors
        [createNodeStub("n1"), createNodeStub("n2")] | []
        []                                           | [createNodeStub("n1"), createNodeStub("n2")]
    }

    def "multi-input/single-output 'sink' node fails validation"(Set<EipNode> predecessors, Set<EipNode> successors) {
        given:
        graph.predecessors(testNode) >> predecessors
        graph.successors(testNode) >> successors

        when:
        transformer.apply(testNode, graph)

        then:
        testNode.connectionType() >> ConnectionType.SINK
        thrown(IllegalArgumentException)

        where:
        predecessors                                 | successors
        [createNodeStub("n1"), createNodeStub("n2")] | []
        []                                           | [createNodeStub("n1")]
    }

    def "multi-output/single-input 'source' node fails validation"(Set<EipNode> predecessors, Set<EipNode> successors) {
        given:
        graph.predecessors(testNode) >> predecessors
        graph.successors(testNode) >> successors

        when:
        transformer.apply(testNode, graph)

        then:
        testNode.connectionType() >> ConnectionType.SOURCE
        thrown(IllegalArgumentException)

        where:
        predecessors           | successors
        [createNodeStub("n1")] | []
        []                     | [createNodeStub("n1"), createNodeStub("n2")]
    }

    def "'tee' node with too many predecessors or successors fails validation"(Set<EipNode> predecessors, Set<EipNode> successors) {
        given:
        graph.predecessors(testNode) >> predecessors
        graph.successors(testNode) >> successors

        when:
        transformer.apply(testNode, graph)

        then:
        testNode.connectionType() >> ConnectionType.TEE

        thrown(IllegalArgumentException)

        where:
        predecessors                                 | successors
        [createNodeStub("n1"), createNodeStub("n1")] | []
        []                                           | [createNodeStub("n1"), createNodeStub("n2"),
                                                        createNodeStub("n3")]
    }

    def "create intermediate channel between two non-channel nodes"() {
        given:
        def target = createNodeStub("target")

        graph.predecessors(testNode) >> []
        graph.successors(testNode) >> [target]

        def channelId = "chan1"
        graph.getEdgeProps(testNode, target) >> createEdgeProps(channelId)

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def elements = transformation.createDirectChannels()

        then:
        elements.size() == 1
        def channel = elements[0]
        channel.prefix() == DIRECT_CHANNEL.namespace()
        channel.localName() == DIRECT_CHANNEL.name()
        channel.attributes() == [(ID): channelId]
        channel.children().isEmpty()
    }

    def "No intermediate channel if source or target node is an explicit 'channel'"(Role sourceRole, Role targetRole) {
        given:
        def source = createNodeStub("source")

        def target = createNodeStub("target")

        graph.predecessors(source) >> []
        graph.successors(source) >> [target]

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(source, graph)
        def elements = transformation.createDirectChannels()

        then:
        source.role() >> sourceRole
        target.role() >> targetRole

        elements.isEmpty()

        where:
        sourceRole    | targetRole
        Role.CHANNEL  | Role.ENDPOINT
        Role.ENDPOINT | Role.CHANNEL
        Role.CHANNEL  | Role.CHANNEL
    }

    def "addChannelAttributes with no predecessors or successors -> attributes unchanged"(ConnectionType connectionType) {
        given:
        graph.predecessors(testNode) >> []
        graph.successors(testNode) >> []

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> connectionType

        attributes.isEmpty()

        where:
        connectionType << ConnectionType.values()
    }

    def "addChannelAttributes with channel node -> attributes unchanged"() {
        given:
        graph.predecessors(testNode) >> [createNodeStub("pre1")]
        graph.successors(testNode) >> [createNodeStub("post1")]

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.role() >> Role.CHANNEL

        attributes.isEmpty()
    }

    def "addChannelAttributes with 'router' node -> input channel attributes added"() {
        given:
        def preNode = createNodeStub("pre1")
        def postNode = createNodeStub("post1")

        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode]

        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("in")
        graph.getEdgeProps(testNode, postNode) >> createEdgeProps("out")

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.CONTENT_BASED_ROUTER

        attributes.size() == 1
        attributes[INPUT_CHANNEL] == "in"
    }

    def "addChannelAttributes with 'passthru' node -> input and output channel attributes added"() {
        given:
        def preNode = createNodeStub("pre1")
        def postNode = createNodeStub("post1")

        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode]

        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("in")
        graph.getEdgeProps(testNode, postNode) >> createEdgeProps("out")

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.PASSTHRU

        attributes.size() == 2
        attributes[INPUT_CHANNEL] == "in"
        attributes[OUTPUT_CHANNEL] == "out"
    }

    def "addChannelAttributes with 'request_reply' node -> input and output channel attributes added"() {
        given:
        def preNode = createNodeStub("pre1")
        def postNode = createNodeStub("post1")

        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode]

        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("in")
        graph.getEdgeProps(testNode, postNode) >> createEdgeProps("out")

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.REQUEST_REPLY

        attributes.size() == 2
        attributes[REQUEST_CHANNEL] == "in"
        attributes[REPLY_CHANNEL] == "out"
    }

    def "addChannelAttributes with 'sink' node -> channel attribute is added"() {
        given:
        def preNode = createNodeStub("pre1")
        graph.predecessors(testNode) >> [preNode]
        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("chanId")

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.SINK

        attributes.size() == 1
        attributes[CHANNEL] == "chanId"
    }

    def "addChannelAttributes with 'source' node -> channel attribute is added"() {
        given:
        def postNode = createNodeStub("pre1")
        graph.successors(testNode) >> [postNode]
        graph.getEdgeProps(testNode, postNode) >> createEdgeProps("chanId")

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.SOURCE

        attributes.size() == 1
        attributes[CHANNEL] == "chanId"
    }

    def "addChannelAttributes with 'tee' node and no discard channel -> input and output channel attributes added"() {
        given:
        def preNode = createNodeStub("pre1")
        def postNode = createNodeStub("post1")

        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode]

        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("in")
        graph.getEdgeProps(testNode, postNode) >> createEdgeProps("out")

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.TEE

        attributes.size() == 2
        attributes[INPUT_CHANNEL] == "in"
        attributes[OUTPUT_CHANNEL] == "out"
    }

    def "addChannelAttributes with 'tee' node and discard channel -> input, output, and discard channel attributes added"() {
        given:
        def preNode = createNodeStub("pre1")
        def postNode = createNodeStub("post1")
        def discardNode = createNodeStub("post2")

        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode, discardNode]

        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("in")
        graph.getEdgeProps(testNode, postNode) >> createEdgeProps("out")
        graph.getEdgeProps(testNode, discardNode) >> Optional.of(new EdgeProps("discard",
                EdgeProps.EdgeType.DISCARD))

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.TEE

        attributes.size() == 3
        attributes[INPUT_CHANNEL] == "in"
        attributes[OUTPUT_CHANNEL] == "out"
        attributes[DISCARD_CHANNEL] == "discard"
    }

    def "addChannelAttributes with 'tee' node and multiple outgoing or discard channels -> fails validation"(EdgeProps.EdgeType edgeType) {
        given:
        def preNode = createNodeStub("pre1")
        def postNode1 = createNodeStub("post1")
        def postNode2 = createNodeStub("post2")

        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode1, postNode2]

        graph.getEdgeProps(preNode, testNode) >> createEdgeProps("in")
        graph.getEdgeProps(testNode, postNode1) >> Optional.of(new EdgeProps("out1",
                edgeType))
        graph.getEdgeProps(testNode, postNode2) >> Optional.of(new EdgeProps("out2",
                edgeType))

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        transformation.createChannelAttributes()

        then:
        testNode.connectionType() >> ConnectionType.TEE

        thrown(IllegalArgumentException)

        where:
        edgeType << [EdgeProps.EdgeType.DEFAULT, EdgeProps.EdgeType.DISCARD]
    }

    def "addChannelAttributes with missing edge props falls back to using node ids"() {
        given:
        def preNode = createNodeStub("pre1")
        graph.predecessors(testNode) >> [preNode]

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then: "edge name should be generated as: 'inputNodeId-outputNodeId'"
        testNode.connectionType() >> ConnectionType.SINK

        attributes.size() == 1
        attributes[CHANNEL] == preNode.id() + "-" + testNode.id()
    }

    def "addChannelAttributes with connection to an explicit channel -> channel attributes match channel node id"() {
        given:
        def preNode = createNodeStub("pre1")
        def postNode = createNodeStub("post1")
        graph.predecessors(testNode) >> [preNode]
        graph.successors(testNode) >> [postNode]

        when:
        def transformation = new DefaultNodeTransformer.DefaultTransformation(testNode, graph)
        def attributes = transformation.createChannelAttributes()

        then:
        preNode.role() >> Role.CHANNEL
        postNode.role() >> Role.CHANNEL

        attributes.size() == 2
        attributes[INPUT_CHANNEL] == "pre1"
        attributes[OUTPUT_CHANNEL] == "post1"
    }

    def "end-to-end default node transformer apply"() {
        EipNode inbound = Stub() {
            id() >> "inbound"
            eipId() >> new EipId(TEST_NS, "inbound-comp")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SOURCE
        }

        EipNode outbound = Stub() {
            id() >> "outbound"
            eipId() >> new EipId(TEST_NS, "outbound-comp")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SINK
        }

        def childNode = new EipChild("child1", ["childAttr": "testval3"], null)
        def attrs = ["testkey1": "testval1"]
        EipNode middle = Stub() {
            id() >> "middle"
            eipId() >> new EipId(TEST_NS, "middle-comp")
            role() >> Role.ROUTER
            connectionType() >> ConnectionType.PASSTHRU
            attributes() >> attrs
            children() >> [childNode]
        }

        graph.predecessors(middle) >> [inbound]
        graph.successors(middle) >> [outbound]
        graph.getEdgeProps(inbound, middle) >> createEdgeProps("input")
        graph.getEdgeProps(middle, outbound) >> createEdgeProps("output")

        when:
        def elements = transformer.apply(middle, graph)

        then:
        elements.size() == 2

        def first = elements[0]
        first.prefix() == TEST_NS
        first.localName() == "middle-comp"
        first.attributes() ==
                attrs + [(ID): "middle", (INPUT_CHANNEL): "input", (OUTPUT_CHANNEL): "output"]
        first.children().size() == 1

        def aChild = first.children()[0]
        aChild.prefix() == TEST_NS
        aChild.localName() == "child1"
        aChild.attributes() == childNode.attributes()
        aChild.children().isEmpty()

        def second = elements[1]
        second.prefix() == DIRECT_CHANNEL.namespace()
        second.localName() == DIRECT_CHANNEL.name()
        second.attributes() == [(ID): "output"]
        second.children().isEmpty()
    }

    EipNode createNodeStub(String nodeId) {
        EipNode stub = Stub {
            id() >> nodeId
            eipId() >> new EipId(TEST_NS, "default-component")
            role() >> Role.TRANSFORMER
            connectionType() >> ConnectionType.PASSTHRU
        }
        return stub
    }

    Optional<EdgeProps> createEdgeProps(String id) {
        return Optional.of(new EdgeProps(id))
    }
}
