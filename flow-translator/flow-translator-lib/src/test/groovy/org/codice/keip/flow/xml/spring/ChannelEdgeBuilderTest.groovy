package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EipChild
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Role
import spock.lang.Specification

import javax.xml.transform.TransformerException

import static org.codice.keip.flow.model.ConnectionType.CONTENT_BASED_ROUTER
import static org.codice.keip.flow.model.ConnectionType.INBOUND_REQUEST_REPLY
import static org.codice.keip.flow.model.ConnectionType.PASSTHRU
import static org.codice.keip.flow.model.ConnectionType.REQUEST_REPLY
import static org.codice.keip.flow.model.ConnectionType.SINK
import static org.codice.keip.flow.model.ConnectionType.SOURCE
import static org.codice.keip.flow.model.ConnectionType.TEE
import static org.codice.keip.flow.xml.spring.AttributeNames.DEFAULT_OUTPUT_CHANNEL_NAME
import static org.codice.keip.flow.xml.spring.AttributeNames.DISCARD_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.INPUT_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.OUTPUT_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.REPLY_CHANNEL
import static org.codice.keip.flow.xml.spring.AttributeNames.REQUEST_CHANNEL
import static org.codice.keip.flow.xml.spring.ComponentIdentifiers.DIRECT_CHANNEL

class ChannelEdgeBuilderTest extends Specification {

    def "no channel nodes -> disconnected graph"() {
        given:
        def nodes = [
                createNode("n1", SOURCE, [:]),
                createNode("n2", SINK, [:])
        ]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 2

        def ec = new EdgeChecker(graph)

        ec.check(nodes[0], [], [])
        ec.check(nodes[1], [], [])
    }

    def "simple connected 3 node graph -> 3 nodes and 2 edges"() {
        given: "n1 -> n2 -> n3"
        def n1 = createNode("n1", SOURCE, ["key1": "val1", "channel": "n1-n2", "key2": "val2"])
        def n2 = createNode("n2",
                PASSTHRU, ["input-channel": "n1-n2", "output-channel": "n2-n3"])
        def n3 = createNode("n3", SINK, ["channel": "n2-n3", "abc": "def"])

        def nodes = [
                n1,
                n2,
                n3,
                *createDirectChannels("n1-n2", "n2-n3"),
        ]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        def outputNodes = graph.traverse().toList()
        outputNodes.size() == 3
        outputNodes[0].attributes() == ["key1": "val1", "key2": "val2"]
        outputNodes[1].attributes().isEmpty()
        outputNodes[2].attributes() == ["abc": "def"]

        def ec = new EdgeChecker(graph)

        ec.check(n1, [], [n2])
        ec.check(n2, [n1], [n3])
        ec.check(n3, [n2], [])
    }

    def "non-existing channel id reference -> throw exception"() {
        given:
        def n1 = createNode("n1", SOURCE, ["channel": "n1-n2"])
        def n2 = createNode("n2", SINK, ["channel": "n1-n2"])

        def nodes = [n1, n2]

        when:
        new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        thrown(TransformerException)
    }

    def "disconnected channel nodes (no inputs or no outputs) -> throw exception"(List<EipNode> nodes, List<EipNode> channels) {
        given:
        def nodeList = [*nodes, *channels]

        when:
        new ChannelEdgeBuilder(nodeList).buildGraph()

        then:
        thrown(TransformerException)

        where:
        nodes                                          | channels
        []                                             | createDirectChannels("discon")
        [createNode("n1", SOURCE, ["channel": "one"])] | (createDirectChannels "one")
        [createNode("n2", SINK, ["channel": "one"])]   | createDirectChannels("one")
    }

    def "test passthru nodes"() {
        given: "source -> transformer -> sink "
        def source = createNode("source", SOURCE, ["channel": "in"])
        def transformer = createNode("transformer", PASSTHRU,
                [(INPUT_CHANNEL) : "in",
                 (OUTPUT_CHANNEL): "out"])
        def sink = createNode("sink", SINK, ["channel": "out"])

        def nodes = [transformer,
                     source,
                     sink,
                     *createDirectChannels("in", "out")]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 3

        def ec = new EdgeChecker(graph)

        ec.check(source, [], [transformer])
        ec.check(transformer, [source], [sink])
        ec.check(sink, [transformer], [])
    }

    def "test 'tee' connection type nodes [e.g. filters]"() {
        given: "source -> filter -> sink | filter -> alt"
        def source = createNode("source", SOURCE, ["channel": "in"])
        def sink = createNode("sink", SINK, ["channel": "out"])
        def alt = createNode("alt", SINK, ["channel": "toDiscard"])
        def filter = createNode("testFilter", TEE,
                [(INPUT_CHANNEL)  : "in",
                 (OUTPUT_CHANNEL) : "out",
                 (DISCARD_CHANNEL): "toDiscard"])

        def nodes = [filter,
                     source,
                     sink,
                     alt,
                     *createDirectChannels("in", "out", "toDiscard")]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 4

        def ec = new EdgeChecker(graph)

        ec.check(source, [], [filter])
        ec.check(filter, [source], [sink, alt])
        ec.check(sink, [filter], [])
        ec.check(alt, [filter], [])
    }

    def "test request-reply nodes"() {
        given: "source -> gateway -> sink"
        def source = createNode("source", SOURCE, ["channel": "in"])
        def sink = createNode("sink", SINK, ["channel": "out"])
        def gateway = createNode("gateway", REQUEST_REPLY,
                [(REQUEST_CHANNEL): "in",
                 (REPLY_CHANNEL)  : "out"])

        def nodes = [source,
                     gateway,
                     sink,
                     *createDirectChannels("in", "out")]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 3

        def ec = new EdgeChecker(graph)

        ec.check(source, [], [gateway])
        ec.check(gateway, [source], [sink])
        ec.check(sink, [gateway], [])
    }

    def "test inbound-request-reply nodes"() {
        given: "gateway -> t1 -> t2 -> gateway"
        def gateway = createNode("gateway", INBOUND_REQUEST_REPLY,
                [(REQUEST_CHANNEL): "one",
                 (REPLY_CHANNEL)  : "three"])
        def t1 = createNode("t1", PASSTHRU,
                [(INPUT_CHANNEL) : "one",
                 (OUTPUT_CHANNEL): "two"])
        def t2 = createNode("t2", PASSTHRU,
                [(INPUT_CHANNEL) : "two",
                 (OUTPUT_CHANNEL): "three"])

        def nodes = [gateway,
                     t1,
                     t2,
                     *createDirectChannels("one", "two", "three")]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 3

        def ec = new EdgeChecker(graph)

        ec.check(gateway, [t2], [t1])
        ec.check(t1, [gateway], [t2])
        ec.check(t2, [t1], [gateway])
    }

    def "test content-based routers with a 'mapping' child"() {
        given: "source -> router -> sink1+sink2+sink3"
        def source = createNode("source", SOURCE, ["channel": "in"])
        def router = createNode("router", CONTENT_BASED_ROUTER,
                ["expression"                 : "headers['status']",
                 (INPUT_CHANNEL)              : "in",
                 (DEFAULT_OUTPUT_CHANNEL_NAME): "out3"])
        def sink1 = createNode("sink1", SINK, ["channel": "out1"])
        def sink2 = createNode("sink2", SINK, ["channel": "out2"])
        def sink3 = createNode("sink3", SINK, ["channel": "out3"])

        EipChild mapping1 = new EipChild("mapping", ["value": "one", "channel": "out1"], [])
        EipChild mapping2 = new EipChild("mapping", ["value": "two", "channel": "out2"], [])
        EipChild other = new EipChild("other", ["key1": "val1"], [])
        router = router.withChildren([mapping1, mapping2, other])

        def nodes = [source,
                     router,
                     sink1,
                     sink2,
                     sink3,
                     *createDirectChannels("in", "out1", "out2", "out3")]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        def outputNodes = graph.traverse().toList()
        outputNodes.size() == 5
        def updatedRouter = outputNodes.find { it == router }
        updatedRouter.attributes() == ["expression": "headers['status']"]

        def ec = new EdgeChecker(graph)

        ec.check(source, [], [router])
        ec.check(router, [source], [sink1, sink2, sink3])
    }

    def "test content-based routers with a 'recipient' child"() {
        given: "source -> recipient-list -> sink1+sink2"
        def source = createNode("source", SOURCE, ["channel": "in"])
        def recList = createNode("recipient-list", CONTENT_BASED_ROUTER, [(INPUT_CHANNEL): "in"])
        def sink1 = createNode("sink1", SINK, ["channel": "out1"])
        def sink2 = createNode("sink2", SINK, ["channel": "out2"])

        EipChild recipient1 = new EipChild("recipient", ["value": "one", "channel": "out1"], [])
        EipChild recipient2 = new EipChild("recipient", ["value": "two", "channel": "out2"], [])
        recList = recList.withChildren([recipient1, recipient2])

        def nodes = [source,
                     recList,
                     sink1,
                     sink2,
                     *createDirectChannels("in", "out1", "out2")]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        def outputNodes = graph.traverse().toList()
        outputNodes.size() == 4
        def updatedRecList = outputNodes.find { it == recList }
        updatedRecList.attributes().isEmpty()

        def ec = new EdgeChecker(graph)

        ec.check(source, [], [recList])
        ec.check(recList, [source], [sink1, sink2])
    }

    def "test content-based routers with a 'mapping' child missing a 'channel' attribute on mapping -> exception"() {
        given: "source -> router -> sink1+sink2+sink3"
        def source = createNode("source", SOURCE, ["channel": "in"])
        def router = createNode("router", CONTENT_BASED_ROUTER,
                ["expression"   : "headers['status']",
                 (INPUT_CHANNEL): "in"])
        def sink1 = createNode("sink1", SINK, ["channel": "out1"])
        def sink2 = createNode("sink2", SINK, ["channel": "out2"])

        EipChild mapping1 = new EipChild("mapping", ["value": "one", "channel": "out1"], [])
        EipChild mapping2 = new EipChild("mapping", ["value": "two"], [])
        router = router.withChildren([mapping1, mapping2])

        def nodes = [source,
                     router,
                     sink1,
                     sink2,
                     *createDirectChannels("in", "out1", "out2")]

        when:
        new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        thrown(TransformerException)
    }

    def "non-direct channel types are left as standalone nodes"(EipId eipId, List<EipChild> children) {
        given:
        def n1 = createNode("n1", SOURCE, ["channel": "one"])
        def n2 = createNode("n2", SINK, ["channel": "one"])
        def chan = new EipNode(
                "one",
                eipId,
                null,
                null,
                Role.CHANNEL,
                PASSTHRU,
                [:],
                children
        )

        def nodes = [n1, n2, chan]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 3

        def ec = new EdgeChecker(graph)
        ec.check(n1, [], [chan])
        ec.check(chan, [n1], [n2])
        ec.check(n2, [chan], [])

        where:
        eipId                                                                         | children
        DIRECT_CHANNEL                                                                | [new EipChild("queue", [:], [])]
        new EipId(Namespaces.INTEGRATION.eipNamespace(), "publish-subscribe-channel") | []
    }

    def "channels with multiple incoming connections are left as standalone nodes"() {
        given: "n1+n2 -> chan -> n3"
        def n1 = createNode("n1", SOURCE, ["channel": "in"])
        def n2 = createNode("n2", SOURCE, ["channel": "in"])
        def n3 = createNode("n3", SINK, ["channel": "in"])

        def nodes = [
                n1,
                n2,
                n3,
                *createDirectChannels("in"),
        ]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 4

        def ec = new EdgeChecker(graph)
        EipNode chan = nodes[3] as EipNode

        ec.check(n1, [], [chan])
        ec.check(n2, [], [chan])
        ec.check(chan, [n1, n2], [n3])
        ec.check(n3, [chan], [])
    }

    def "channels with multiple outgoing connections are left as standalone nodes"() {
        given: "n1 -> chan -> n2+n3"
        def n1 = createNode("n1", SOURCE, ["channel": "in"])
        def n2 = createNode("n2", SINK, ["channel": "in"])
        def n3 = createNode("n3", SINK, ["channel": "in"])

        def nodes = [
                n1,
                n2,
                n3,
                *createDirectChannels("in"),
        ]

        when:
        def graph = new ChannelEdgeBuilder(nodes).buildGraph()

        then:
        graph.traverse().count() == 4

        def ec = new EdgeChecker(graph)
        EipNode chan = nodes[3] as EipNode

        ec.check(n1, [], [chan])
        ec.check(chan, [n1], [n2, n3])
        ec.check(n2, [chan], [])
        ec.check(n3, [chan], [])
    }

    EipNode createNode(String nodeId, ConnectionType connType, Map<String, Object> attrs) {
        return new EipNode(nodeId,
                new EipId("test-ns", "test-name"),
                null,
                null,
                null,
                connType,
                attrs,
                null)
    }

    List<EipNode> createDirectChannels(String... channelIds) {
        channelIds.collect { String channelId ->
            Stub(EipNode) {
                id() >> channelId
                connectionType() >> PASSTHRU
                role() >> Role.CHANNEL
                eipId() >> DIRECT_CHANNEL
            }
        }
    }
}
