package org.codice.keip.flow.xml.spring

import com.ctc.wstx.msv.W3CMultiSchemaFactory
import org.codehaus.stax2.validation.XMLValidationSchema
import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EdgeProps
import org.codice.keip.flow.model.EipChild
import org.codice.keip.flow.model.EipGraph
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Role
import org.codice.keip.flow.xml.NamespaceSpec
import org.codice.keip.flow.xml.NodeTransformer
import org.w3c.dom.Node
import org.xmlunit.builder.Input
import org.xmlunit.xpath.JAXPXPathEngine
import spock.lang.Specification

import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.stream.StreamSource
import java.nio.file.Path
import java.util.stream.Stream

import static org.codice.keip.flow.xml.XmlComparisonUtil.compareXml
import static org.codice.keip.flow.xml.XmlComparisonUtil.readTestXml

class IntegrationGraphTransformerTest extends Specification {

    private static final List<NamespaceSpec> NAMESPACES = [new NamespaceSpec("jms", "http://www.springframework.org/schema/integration/jms", "https://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd")]

    EipGraph graph = Stub()

    def xmlOutput = new StringWriter()

    def graphTransformer = IntegrationGraphTransformer.createDefaultInstance(NAMESPACES)

    def "Transform empty graph. Check root element"() {
        given:
        def errors = graphTransformer.toXml(graph, xmlOutput)

        expect:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), readTestXml("empty.xml"))
    }

    def "Transform single node graph"() {
        given:
        EipNode node = Stub {
            id() >> "test-id"
            eipId() >> new EipId("jms", "inbound-channel-adapter")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SOURCE
            attributes() >> ["pub-sub-domain": "true"]
            children() >> [new EipChild("poller", ["fixed-delay": 1000], null)]
        }

        graph.traverse() >> { _ -> Stream.of(node) }

        when:
        def errors = graphTransformer.toXml(graph, xmlOutput)

        then: "only referenced namespaces should be included (instead of all registered namespaces)"
        errors.isEmpty()
        compareXml(xmlOutput.toString(), readTestXml("single-node.xml"))
    }

    def "Transform multi-node graph"() {
        given: "inbound adapter -> transformer -> outbound adapter"
        EipNode inbound = Stub {
            id() >> "messageGenerator"
            eipId() >> new EipId("integration", "inbound-channel-adapter")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SOURCE
            attributes() >> ["expression": "'TestMessage'"]
            children() >> [new EipChild("poller", ["fixed-rate": 5000], null)]
        }

        EipNode transformer = Stub {
            id() >> "appender"
            eipId() >> new EipId("integration", "transformer")
            role() >> Role.TRANSFORMER
            connectionType() >> ConnectionType.PASSTHRU
            attributes() >> ["expression": "payload + ' processed'"]
        }

        EipNode outbound = Stub {
            id() >> "jmsProducer"
            eipId() >> new EipId("jms", "outbound-channel-adapter")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SINK
            attributes() >> ["destination-name": "test-target"]
        }

        graph.predecessors(inbound) >> []
        graph.successors(inbound) >> [transformer]
        graph.getEdgeProps(inbound, transformer) >> createEdgeProps("messageSource")

        graph.predecessors(transformer) >> [inbound]
        graph.successors(transformer) >> [outbound]
        graph.getEdgeProps(transformer, outbound) >> createEdgeProps("transformerOut")

        graph.predecessors(outbound) >> [transformer]
        graph.successors(outbound) >> []

        graph.traverse() >> { _ -> Stream.of(inbound, transformer, outbound) }

        when:
        def errors = graphTransformer.toXml(graph, xmlOutput)

        then:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), readTestXml("multi-node.xml"))
    }

    def "Transforming node with a unregistered EIP namespace throws an exception"() {
        given:
        EipNode node = Stub {
            id() >> "test-id"
            eipId() >> new EipId("unknown", "simple")
            role() >> Role.TRANSFORMER
            connectionType() >> ConnectionType.PASSTHRU
        }

        graph.traverse() >> { _ -> Stream.of(node) }

        when:
        graphTransformer.toXml(graph, xmlOutput)

        then:
        thrown(TransformerException)
    }

    def "Initializing transformer with a reserved namespace prefix throws an exception"(String prefix) {
        given:
        def namespaces = [new NamespaceSpec(prefix,
                "http://www.example.com/schema/xml",
                "https://www.example.com/schema/xml")]

        when:
        IntegrationGraphTransformer.createDefaultInstance(namespaces)

        then:
        thrown(IllegalArgumentException)

        where:
        prefix << ["xml", "xsi", "beans", "integration"]
    }

    def "Registering custom node transformers resolves correctly"() {
        given: "inbound adapter -> outbound adapter"
        EipNode inbound = Stub {
            id() >> "inbound"
            eipId() >> new EipId("integration", "inbound-adapter")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SOURCE
        }

        def outboundEipId = new EipId("integration", "outbound-adapter")
        EipNode outbound = Stub {
            id() >> "outbound"
            eipId() >> outboundEipId
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SINK
        }

        graph.predecessors(inbound) >> []
        graph.successors(inbound) >> [outbound]
        graph.getEdgeProps(inbound, outbound) >> createEdgeProps("chan1")

        graph.predecessors(outbound) >> [inbound]
        graph.successors(outbound) >> []

        graph.traverse() >> { _ -> Stream.of(inbound, outbound) }

        NodeTransformer mockTransformer = Mock()

        when:
        def graphTransformer =
                IntegrationGraphTransformer.createInstance(NAMESPACES,
                        (factory) ->
                                factory.registerNodeTransformer(outboundEipId, mockTransformer))
        def errors = graphTransformer.toXml(graph, xmlOutput)

        then:
        errors.isEmpty()
        1 * mockTransformer.apply(outbound, graph) >> []
    }

    def "NodeTransformer throws exception -> add exception to error list and move on to next node"() {
        given: "inbound adapter -> outbound adapter"
        def inboundEipId = new EipId("integration", "inbound-adapter")
        EipNode inbound = Stub {
            id() >> "inbound"
            eipId() >> inboundEipId
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SOURCE
        }

        EipNode outbound = Stub {
            id() >> "outbound"
            eipId() >> new EipId("integration", "outbound-adapter")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SINK
        }

        graph.predecessors(inbound) >> []
        graph.successors(inbound) >> [outbound]
        graph.getEdgeProps(inbound, outbound) >> createEdgeProps("chan1")

        graph.predecessors(outbound) >> [inbound]
        graph.successors(outbound) >> []

        graph.traverse() >> { _ -> Stream.of(inbound, outbound) }

        NodeTransformer mockTransformer = Mock()

        when:
        def graphTransformer =
                IntegrationGraphTransformer.createInstance(NAMESPACES,
                        (factory) ->
                                factory.registerNodeTransformer(inboundEipId, mockTransformer))
        def errors = graphTransformer.toXml(graph, xmlOutput)

        then:
        1 * mockTransformer.apply(inbound,
                graph) >> { throw new RuntimeException("inbound transformer error") }

        errors.size() == 1
        errors[0].source() == "inbound"

        def rootElem = getRootElement(xmlOutput.toString())
        rootElem.getLocalName() == "beans"
        def outboundElem = rootElem.getFirstChild()
        outboundElem.getLocalName() == "outbound-adapter"
        outboundElem.getNextSibling() == null
    }

    def "Transform single node graph with custom entities"() {
        given:
        EipNode node = Stub {
            id() >> "test-id"
            eipId() >> new EipId("jms", "inbound-channel-adapter")
            role() >> Role.ENDPOINT
            connectionType() >> ConnectionType.SOURCE
            attributes() >> ["pub-sub-domain": "true"]
            children() >> [new EipChild("poller", ["fixed-delay": 1000], null)]
        }

        graph.traverse() >> { _ -> Stream.of(node) }

        when:
        def errors = graphTransformer.toXml(graph, xmlOutput, generateCustomEntities())

        then:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), readTestXml("single-node-with-custom-entities.xml"))
    }

    def "Transform custom entities only"() {
        given:
        graph.traverse() >> { _ -> Stream.empty() }

        when:
        def errors = graphTransformer.toXml(graph, xmlOutput, generateCustomEntities())

        then:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), readTestXml("custom-entities-only.xml"))
    }

    def "test xsd validation"(String xmlFilePath) {
        given:
        Reader xml = readTestXml(xmlFilePath).newReader()

        when:
        graphTransformer.fromXml(xml, createValidationSchema())

        then:
        noExceptionThrown()

        where:
        xmlFilePath << ["default-namespaces.xml", "nested-children.xml"]
    }

    Optional<EdgeProps> createEdgeProps(String id) {
        return Optional.of(new EdgeProps(id))
    }

    Node getRootElement(String xml) {
        def matches = new JAXPXPathEngine().selectNodes("/*",
                Input.fromString(xml).build())
        return matches.toList()[0]
    }

    Map<String, String> generateCustomEntities() {
        return ["e1": '<bean class="com.example.Test"><property name="limit" value="65536" /></bean>',
                "e2": '<arbitrary>test</arbitrary>']
    }

    XMLValidationSchema createValidationSchema() {
        String baseUri = "classpath:/schemas/dummy"

        LinkedHashMap<String, Source> schemas = [
                "http://www.springframework.org/schema/beans"      : getXsd("spring-beans.xsd"),
                "http://www.springframework.org/schema/tool"       : getXsd("spring-tool.xsd"),
                "http://www.springframework.org/schema/integration":
                        getXsd("spring-integration-5.2.xsd")]

        return new W3CMultiSchemaFactory().createSchema(baseUri, schemas)
    }

    // TODO: Fetch schemas from dependency JAR instead
    StreamSource getXsd(String filename) {
        String xsdPath = Path.of("schemas", filename).toString();
        StreamSource s = new StreamSource(getClass().getClassLoader().getResourceAsStream(xsdPath));
        s.setSystemId(
                Objects
                        .requireNonNull(getClass().getClassLoader().getResource(xsdPath))
                        .toExternalForm());
        return s;
    }
}
