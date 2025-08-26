package org.codice.keip.flow.graph

import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EdgeProps
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Flow
import org.codice.keip.flow.model.FlowEdge
import org.codice.keip.flow.model.Role
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

class GuavaGraphTest extends Specification {

    def "empty flow returns an empty node stream"() {
        when:
        def graph = GuavaGraph.from(new Flow([], []))
        def nodes = graph.traverse()

        then:
        nodes.toList().isEmpty()
    }

    def "detached edge throws exception"(String sourceId, String targetId) {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")

        def e1 = new FlowEdge("a", sourceId, targetId)

        def flow = new Flow([n1, n2], [e1])

        when:
        GuavaGraph.from(flow)

        then:
        thrown(IllegalArgumentException)

        where:
        sourceId | targetId
        "0"      | "20"
        "1"      | "5"
        "5"      | "2"
    }

    def "multiple single element flows"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")

        def flow = new Flow([n1, n2], [])
        def graph = GuavaGraph.from(flow)

        when:
        def nodes = graph.traverse()

        then:
        getIds(nodes) == ["1", "2"]
        graph.predecessors(n1).isEmpty()
        graph.successors(n1).isEmpty()
    }

    /**
     * 1 -> 2 -> 3
     * 4 -> 5
     */
    def "multiple flows"() {
        given:
        def start1 = newNode("1")
        def middle1 = newNode("2")
        def end1 = newNode("3")

        def start2 = newNode("4")
        def end2 = newNode("5")

        def edge1 = new FlowEdge("a", start1.id(), middle1.id())
        def edge2 = new FlowEdge("b", middle1.id(), end1.id())
        def edge3 = new FlowEdge("c", start2.id(), end2.id())

        def flow = new Flow([start1, middle1, end1, start2, end2], [edge1, edge2, edge3])
        def graph = GuavaGraph.from(flow)

        when:
        def nodes = graph.traverse()

        then:
        getIds(nodes) == ["1", "2", "3", "4", "5"]
    }

    /**
     * 1 -- 2
     * |
     * 3
     */
    def "predecessor and successor sanity checks"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")
        def n3 = newNode("3")

        def edgeA = new FlowEdge("a", n1.id(), n2.id())
        def edgeB = new FlowEdge("b", n1.id(), n3.id())

        def flow = new Flow([n1, n2, n3], [edgeA, edgeB])
        def graph = GuavaGraph.from(flow)

        expect:
        graph.predecessors(n1).isEmpty()
        graph.successors(n1).toList() == [n2, n3]

        graph.predecessors(n2).toList() == [n1]
        graph.successors(n2).isEmpty()

        graph.predecessors(n3).toList() == [n1]
        graph.successors(n3).isEmpty()
    }

    /**
     * 1 -- 2 -- 3
     * |
     * 4
     */
    def "EdgeProps sanity check"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")
        def n3 = newNode("3")
        def n4 = newNode("4")

        def edgeA = new FlowEdge("a", n1.id(), n2.id())
        def edgeB = new FlowEdge("b", n2.id(), n3.id(), null)
        def edgeC = new FlowEdge("c", n1.id(), n4.id(), EdgeProps.EdgeType.DISCARD)

        def flow = new Flow([n1, n2, n3, n4], [edgeA, edgeB, edgeC])
        def graph = GuavaGraph.from(flow)

        when:
        def edgePropsA = graph.getEdgeProps(n1, n2).get()
        def edgePropsB = graph.getEdgeProps(n2, n3).get()
        def edgePropsC = graph.getEdgeProps(n1, n4).get()

        then:
        edgePropsA.id() == edgeA.id()
        edgePropsA.type() == EdgeProps.EdgeType.DEFAULT

        edgePropsB.id() == edgeB.id()
        edgePropsB.type() == EdgeProps.EdgeType.DEFAULT

        edgePropsC.id() == edgeC.id()
        edgePropsC.type() == EdgeProps.EdgeType.DISCARD
    }

    def "duplicate node ids throws exception"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")
        def n3 = newNode("1")

        def flow = new Flow([n1, n2, n3], [])

        when:
        GuavaGraph.from(flow)

        then:
        thrown(IllegalArgumentException)
    }

    def "adding a parallel edge overwrites the older one"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")

        def originalEdge = new FlowEdge("a", n1.id(), n2.id())
        def parallelEdge = new FlowEdge("b", n1.id(), n2.id())

        def flow = new Flow([n1, n2], [originalEdge, parallelEdge])

        when:
        def graph = GuavaGraph.from(flow)

        then:
        graph.getEdgeProps(n1, n2).get().id() == parallelEdge.id()
    }

    def "graph with a single cycle -> each node is traversed once"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")
        def n3 = newNode("3")

        def e1 = new FlowEdge("a", n1.id(), n2.id())
        def e2 = new FlowEdge("b", n2.id(), n3.id())
        def e3 = new FlowEdge("c", n3.id(), n1.id())

        def flow = new Flow([n1, n2, n3], [e1, e2, e3])

        when:
        def graph = GuavaGraph.from(flow)

        then:
        graph.traverse().map { it.id() }.collect(Collectors.toSet()).size() == 3
    }

    def "graph with multiple cycles is allowed -> each node is traversed once"() {
        given:
        def n1 = newNode("1")
        def n2 = newNode("2")
        def n3 = newNode("3")

        def n4 = newNode("4")
        def n5 = newNode("5")
        def n6 = newNode("6")

        // cycle free connected component
        def n7 = newNode("7")
        def n8 = newNode("8")

        def e1 = new FlowEdge("a", n1.id(), n2.id())
        def e2 = new FlowEdge("b", n2.id(), n3.id())
        def e3 = new FlowEdge("c", n3.id(), n1.id())

        def e4 = new FlowEdge("d", n4.id(), n5.id())
        def e5 = new FlowEdge("e", n5.id(), n6.id())
        def e6 = new FlowEdge("f", n6.id(), n4.id())

        def e7 = new FlowEdge("g", n7.id(), n8.id())

        def flow = new Flow([n1, n2, n3, n4, n5, n6, n7, n8], [e1, e2, e3, e4, e5, e6, e7])

        when:
        def graph = GuavaGraph.from(flow)

        then:
        graph.traverse().toList().size() == 8
    }

    /**
     * 1 -> 2 -> 3
     * 4 -> 5
     */
    def "multiple flows graph - convert to and from EIP Flow"() {
        given:
        def start1 = newNode("1")
        def middle1 = newNode("2")
        def end1 = newNode("3")

        def start2 = newNode("4")
        def end2 = newNode("5")

        def edge1 = new FlowEdge("a", start1.id(), middle1.id())
        def edge2 = new FlowEdge("b", middle1.id(), end1.id())
        def edge3 = new FlowEdge("c", start2.id(), end2.id(), EdgeProps.EdgeType.DISCARD)

        def flow = new Flow([start1, middle1, end1, start2, end2], [edge1, edge2, edge3])
        def graph = GuavaGraph.from(flow)

        when:
        def resultFlow = graph.toFlow()

        then:
        resultFlow == flow
    }

    private static EipNode newNode(String id) {
        return new EipNode(
                id, new EipId("test", "a"), null, null,
                Role.TRANSFORMER, ConnectionType.PASSTHRU, null, null)
    }

    private static List<String> getIds(Stream<EipNode> nodes) {
        return nodes.map { it.id() }.toList()
    }
}
