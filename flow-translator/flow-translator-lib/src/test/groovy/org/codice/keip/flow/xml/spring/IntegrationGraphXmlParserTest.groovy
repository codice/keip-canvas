package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.ComponentRegistry
import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Role
import org.codice.keip.flow.xml.NamespaceSpec
import org.codice.keip.flow.xml.XmlElement
import org.codice.keip.flow.xml.XmlElementTransformer
import org.xml.sax.SAXException
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.transform.TransformerException

import static org.codice.keip.flow.ComponentRegistryIO.readComponentDefinitionJson
import static org.codice.keip.flow.xml.XmlComparisonUtil.readTestXml

class IntegrationGraphXmlParserTest extends Specification {

    private static final List<NamespaceSpec> NAMESPACES = [
            new NamespaceSpec("integration", "http://www.springframework.org/schema/integration", "https://www.springframework.org/schema/integration/spring-integration.xsd"),
            new NamespaceSpec("jms", "http://www.springframework.org/schema/integration/jms", "https://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd")
    ]

    @Shared
    def componentRegistry = ComponentRegistry.fromJson(readComponentDefinitionJson())

    @Shared
    def springIntegrationSchema = SchemaBuilder.buildSpringIntegrationSchemas()

    @Shared
    def xmlParser = initParser()

    def "parse xml to graph"() {
        given:
        InputStream xml = readTestXml("nested-children.xml")

        when:
        def result = xmlParser.fromXml(xml)

        then:
        result.customEntities().isEmpty()
        def graph = result.graph()
        def nodes = graph.traverse().toList()
        nodes.size() == 4

        def inAdapter = nodes[0]
        with(inAdapter) {
            id() == "messageGenerator"
            eipId() == new EipId("integration", "inbound-channel-adapter")
            attributes() == ["expression": "'TestMessage'"]
            children().size() == 2
        }

        def poller = inAdapter.children() getFirst()

        with(poller) {
            eipId() == new EipId(Namespaces.INTEGRATION.eipNamespace(), "poller")
            attributes() == ["fixed-rate": "5000"]
            children().size() == 1
        }

        def advice = poller.children().getFirst()
        with(advice) {
            eipId() == new EipId(Namespaces.INTEGRATION.eipNamespace(), "advice-chain")
            attributes().isEmpty()
            children().size() == 1
        }

        def ref = advice.children().getFirst()
        with(ref) {
            eipId() == new EipId(Namespaces.INTEGRATION.eipNamespace(), "ref")
            attributes() == ["bean": "adviceRef"]
            children().isEmpty()
        }

        def expr = inAdapter.children().getLast()
        with(expr) {
            eipId() == new EipId(Namespaces.INTEGRATION.eipNamespace(), "expression")
            attributes() == ["key": "testExp"]
            children().isEmpty()
        }

        def filter = nodes[1]
        with(filter) {
            id() == "checkPayload"
            eipId() == new EipId("integration", "filter")
            attributes() == ["expression": "payload == 'error'"]
            children().isEmpty()
        }

        def outAdapter = nodes[2]
        with(outAdapter) {
            id() == "resultSender"
            eipId() == new EipId("integration", "outbound-channel-adapter")
            attributes().isEmpty()
            children().isEmpty()
        }

        def logAdapter = nodes[3]
        with(logAdapter) {
            id() == "testLogger"
            eipId() == new EipId("integration", "logging-channel-adapter")
            attributes().isEmpty()
            children().isEmpty()
        }

        def ec = new EdgeChecker(graph)
        ec.check(inAdapter, [], [filter])
        ec.check(filter, [inAdapter], [outAdapter, logAdapter])
        ec.check(outAdapter, [filter], [])
        ec.check(logAdapter, [filter], [])
    }

    def "parse xml to graph with no configured validation"() {
        given:
        def localParser = new IntegrationGraphXmlParser(NAMESPACES, componentRegistry)
        InputStream xml = readTestXml("nested-children.xml")

        when:
        def result = localParser.fromXml(xml)

        then:
        result.customEntities().isEmpty()
        def nodes = result.graph().traverse().toList()

        def nodeIds = nodes.stream().map { it.id() }.toList()
        nodeIds == ["messageGenerator", "checkPayload", "resultSender", "testLogger"]

        def inAdapter = nodes[0]
        with(inAdapter) {
            id() == "messageGenerator"
            eipId() == new EipId("integration", "inbound-channel-adapter")
            attributes() == ["expression": "'TestMessage'"]
            children().size() == 2
        }
    }

    def "parse xml with a custom XmlElementTransformer"() {
        given:
        EipNode dummyNode = Stub(EipNode) {
            id() >> "testNode"
            eipId() >> new EipId("test", "dummy")
            role() >> Role.TRANSFORMER
            connectionType() >> ConnectionType.PASSTHRU
        }

        XmlElementTransformer dummyTransformer =
                (XmlElement element, ComponentRegistry registry) -> dummyNode

        def localParser = new IntegrationGraphXmlParser(
                NAMESPACES, componentRegistry, dummyTransformer)

        InputStream xml = readTestXml("single-node.xml")

        when:
        def result = localParser.fromXml(xml)

        then:
        result.customEntities().isEmpty()
        def nodes = result.graph().traverse().toList()
        nodes.size() == 1
        nodes.getFirst() == dummyNode
    }

    def "xml to graph with custom entity"() {
        given:
        InputStream xml = readTestXml("single-node-with-custom-entities.xml")

        when:
        def result = xmlParser.fromXml(xml)

        then:
        def graph = result.graph()
        def nodes = graph.traverse().toList()
        nodes.size() == 1
        nodes.getFirst().id() == "test-id"

        def expectedEntities = [
                "e1": '<bean class="com.example.Test"><property name="limit" value="65536"/></bean>',
                "e2": '<bean lazy-init="true"/>']

        result.customEntities() == expectedEntities
    }

    def "xml to graph with only custom entities, no eip nodes"() {
        given:
        InputStream xml = readTestXml("custom-entities-only.xml")

        when:
        def result = xmlParser.fromXml(xml)

        then:
        def graph = result.graph()
        graph.traverse().count() == 0

        def expectedEntities = [
                "e1": '<bean class="com.example.Test"><property name="limit" value="65536"/></bean>',
                "e2": '<bean lazy-init="true"/>']

        result.customEntities() == expectedEntities
    }

    def "xml to graph with id-less custom entity -> exception thrown"() {
        given:
        InputStream xml = readTestXml("invalid/custom-entity-no-id.xml")

        when:
        xmlParser.fromXml(xml)

        then:
        thrown(TransformerException)
    }

    def "xml has an element unregistered namespace"() {
        given:
        InputStream xml = readTestXml("invalid/unknown-namespace.xml")
        def localParser = new IntegrationGraphXmlParser(NAMESPACES, componentRegistry)

        when:
        localParser.fromXml(xml)

        then:
        thrown(TransformerException)
    }

    def "xml to graph with alternative namespace prefixes"() {
        given:
        InputStream xml = readTestXml("alternative-prefixes.xml")

        when:
        def result = xmlParser.fromXml(xml)

        then:
        def graph = result.graph()
        def nodes = graph.traverse().toList()
        nodes.size() == 2

        def inAdapter = nodes.getFirst()
        with(inAdapter) {
            id() == "messageGenerator"
            eipId() == new EipId("integration", "inbound-channel-adapter")
            attributes() == ["expression": "'TestMessage'"]
            children().size() == 1
        }

        def outAdapter = nodes.getLast()
        with(outAdapter) {
            id() == "jmsProducer"
            eipId() == new EipId("jms", "outbound-channel-adapter")
            attributes() == ["destination-name": "test-target"]
            children().isEmpty()
        }

        def expectedEntities = ["b1": '<bean lazy-init="true"/>']
        result.customEntities() == expectedEntities
    }

    def "xml to graph with missing element ids -> random ids generated"() {
        given:
        InputStream xml = readTestXml("multi-node-missing-ids.xml")

        when:
        def result = xmlParser.fromXml(xml)

        then:
        def graph = result.graph()
        def nodes = graph.traverse().toList()
        nodes.size() == 3

        def inAdapter = nodes[0]
        with(inAdapter) {
            id().size() == 10
            eipId() == new EipId("integration", "inbound-channel-adapter")
            attributes() == ["expression": "'TestMessage'"]
            children().size() == 1
        }

        def transformer = nodes[1]
        with(transformer) {
            id().size() == 10
            eipId() == new EipId("integration", "transformer")
            attributes() == ["expression": "payload + ' processed'"]
            children().isEmpty()
        }

        def outAdapter = nodes[2]
        with(outAdapter) {
            id().size() == 10
            eipId() == new EipId("jms", "outbound-channel-adapter")
            attributes() == ["destination-name": "test-target"]
            children().isEmpty()
        }
    }

    def "xsd validation with invalid xml -> failure"(String xmlFilePath) {
        given:
        InputStream xml = readTestXml(xmlFilePath)
        xmlParser.setValidationSchema(springIntegrationSchema)

        when:
        xmlParser.fromXml(xml)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.cause instanceof SAXException

        where:
        xmlFilePath << [
                "invalid/unknown-attribute.xml",
                "invalid/unknown-element.xml",
                "invalid/unknown-namespace.xml",
        ]
    }

    private IntegrationGraphXmlParser initParser() {
        def parser = new IntegrationGraphXmlParser(NAMESPACES, componentRegistry)
        parser.setValidationSchema(springIntegrationSchema)
        return parser
    }
}