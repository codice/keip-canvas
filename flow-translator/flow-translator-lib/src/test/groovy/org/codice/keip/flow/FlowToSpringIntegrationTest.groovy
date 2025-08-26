package org.codice.keip.flow

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.Flow
import org.codice.keip.flow.xml.NamespaceSpec
import org.codice.keip.flow.xml.NodeTransformer
import org.codice.keip.flow.xml.spring.DefaultNodeTransformer
import org.codice.keip.flow.xml.spring.IntegrationGraphXmlParser
import org.codice.keip.flow.xml.spring.IntegrationGraphXmlSerializer
import org.codice.keip.flow.xml.spring.SchemaBuilder
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

import static ComponentRegistryIO.readComponentDefinitionJson
import static org.codice.keip.flow.xml.XmlComparisonUtil.compareXml
import static org.codice.keip.flow.xml.XmlComparisonUtil.readTestXml

class FlowToSpringIntegrationTest extends Specification {
    private static final List<NamespaceSpec> NAMESPACES_SERIALIZER = [
            new NamespaceSpec("jms", "http://www.springframework.org/schema/integration/jms", "https://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd"),
            new NamespaceSpec("http", "http://www.springframework.org/schema/integration/http", "https://www.springframework.org/schema/integration/http/spring-integration-http.xsd"),
            new NamespaceSpec("ftp", "http://www.springframework.org/schema/integration/ftp", "https://www.springframework.org/schema/integration/ftp/spring-integration-ftp.xsd")
    ]

    private static final List<NamespaceSpec> NAMESPACES_PARSER = [
            new NamespaceSpec("integration", "http://www.springframework.org/schema/integration", "https://www.springframework.org/schema/integration/spring-integration.xsd"),
            *NAMESPACES_SERIALIZER]

    private static final JsonMapper MAPPER =
            JsonMapper.builder()
                      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                      .build()

    @Shared
    def componentRegistry = ComponentRegistry.fromJson(readComponentDefinitionJson())

    @Shared
    def springIntegrationSchema = SchemaBuilder.buildSpringIntegrationSchemas()

    def "End-to-end basic flow to spring-integration xml"(String flowFile, String xmlFile) {
        given:
        def flow = MAPPER.readValue(getFlowJson(flowFile), Flow.class)

        def flowTranslator = new FlowTranslator(new IntegrationGraphXmlSerializer(
                NAMESPACES_SERIALIZER))

        when:
        def output = new StringWriter()
        def errors = flowTranslator.toXml(flow, output)

        then:
        errors.isEmpty()
        compareXml(output.toString(), readTestXml(xmlFile))

        where:
        flowFile          | xmlFile
        "flowGraph1.json" | Path.of("end-to-end", "spring-integration-1.xml").toString()
        "flowGraph2.json" | Path.of("end-to-end", "spring-integration-2.xml").toString()
        "flowGraph3.json" | Path.of("end-to-end", "spring-integration-3.xml").toString()
        "flowGraph4.json" | Path.of("end-to-end", "spring-integration-4.xml").toString()
        "flowGraph5.json" | Path.of("end-to-end", "spring-integration-5.xml").toString()
        "flowGraph6.json" | Path.of("end-to-end", "spring-integration-6.xml").toString()
    }

    def "Verify transformation error list is populated on node transformation error"() {
        given:
        def flow = MAPPER.readValue(getFlowJson("flowGraph1.json"), Flow.class)

        def adapterId = new EipId("integration", "inbound-channel-adapter")

        def graphTransformer = new IntegrationGraphXmlSerializer(
                NAMESPACES_SERIALIZER, buildExceptionalTransformer(adapterId))
        def flowTranslator = new FlowTranslator(graphTransformer)

        when:
        def output = new StringWriter()
        def errors = flowTranslator.toXml(flow, output)

        then:
        errors.size() == 1
    }

    def "End-to-end spring-integration xml to Flow"(String flowFile, String xmlFile) {
        given:
        def xml = readTestXml(xmlFile)
        def xmlParser = new IntegrationGraphXmlParser(NAMESPACES_PARSER, componentRegistry)
        xmlParser.setValidationSchema(springIntegrationSchema)
        def flowTranslator = new FlowTranslator(xmlParser)

        when:
        def result = flowTranslator.fromXml(xml)

        then:
        def expectedFlow = MAPPER.readValue(getFlowJson(flowFile), Flow.class)

        compareFlows(result, expectedFlow)

        where:
        flowFile          | xmlFile
        "flowGraph1.json" | Path.of("end-to-end", "spring-integration-1.xml").toString()
        "flowGraph2.json" | Path.of("end-to-end", "spring-integration-2.xml").toString()
        "flowGraph3.json" | Path.of("end-to-end", "spring-integration-3.xml").toString()
        "flowGraph4.json" | Path.of("end-to-end", "spring-integration-4.xml").toString()
        "flowGraph5.json" | Path.of("end-to-end", "spring-integration-5.xml").toString()
        "flowGraph6.json" | Path.of("end-to-end", "spring-integration-6.xml").toString()
    }

    def "Uninitialized serializer -> UnsupportedOperationException"() {
        given:
        def xmlParser = new IntegrationGraphXmlParser(NAMESPACES_PARSER, componentRegistry)
        def flowTranslator = new FlowTranslator(xmlParser)

        Flow flow = Stub()
        Writer writer = Stub()

        when:
        flowTranslator.toXml(flow, writer)

        then:
        thrown(UnsupportedOperationException)
    }

    def "Uninitialized parser -> UnsupportedOperationException"() {
        given:
        def serializer = new IntegrationGraphXmlSerializer(NAMESPACES_SERIALIZER)
        def flowTranslator = new FlowTranslator(serializer)

        InputStream is = Stub()

        when:
        flowTranslator.fromXml(is)

        then:
        thrown(UnsupportedOperationException)
    }

    def "Full initialization of parser and serializer"() {
        given:
        def serializer = new IntegrationGraphXmlSerializer(NAMESPACES_SERIALIZER)
        def parser = new IntegrationGraphXmlParser(NAMESPACES_PARSER, componentRegistry)
        def flowTranslator = new FlowTranslator(serializer, parser)

        def flow = MAPPER.readValue(getFlowJson("flowGraph1.json"), Flow.class)
        Writer writer = new StringWriter()

        when:
        def errors = flowTranslator.toXml(flow, writer)

        InputStream is = new ByteArrayInputStream(writer.toString().getBytes())
        def resultFlow = flowTranslator.fromXml(is)

        then:
        errors.isEmpty()
        compareFlows(resultFlow, flow)
    }

    static BufferedReader getFlowJson(String filename) {
        Path path = Path.of("json").resolve(filename)
        return FlowToSpringIntegrationTest.class
                                          .getClassLoader()
                                          .getResource(path.toString())
                                          .newReader()
    }

    NodeTransformer buildExceptionalTransformer(EipId errorTrigger) {
        return (node, graph) -> {
            if (node.eipId() == errorTrigger) {
                throw new RuntimeException("${node.id()} transformer error")
            }
            return new DefaultNodeTransformer().apply(node, graph)
        }
    }

    // order insensitive comparison
    boolean compareFlows(Flow first, Flow second) {
        return first.nodes().toSorted() == second.nodes().toSorted() &&
                first.edges().toSorted() == second.edges().toSorted() &&
                first.customEntities() == second.customEntities()
    }
}
