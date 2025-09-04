package org.codice.keip.flow.xml.spring

import com.ctc.wstx.msv.W3CMultiSchemaFactory
import org.codehaus.stax2.validation.XMLValidationSchema
import org.codice.keip.flow.ComponentRegistry
import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.Role
import org.codice.keip.flow.xml.NamespaceSpec
import spock.lang.Specification

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import java.nio.file.Path

import static org.codice.keip.flow.xml.XmlComparisonUtil.readTestXml
import static org.codice.keip.flow.xml.spring.Namespaces.INTEGRATION

// TODO: Test with invalid xml against schema
class IntegrationGraphXmlParserTest extends Specification {

    private static final List<NamespaceSpec> NAMESPACES = [
            new NamespaceSpec("integration", "http://www.springframework.org/schema/integration", "https://www.springframework.org/schema/integration/spring-integration.xsd"),
            new NamespaceSpec("jms", "http://www.springframework.org/schema/integration/jms", "https://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd")
    ]

    ComponentRegistry componentRegistry = buildRegistryStub()

    def xmlParser = new IntegrationGraphXmlParser(NAMESPACES, componentRegistry)

    def "test xsd validation"(String xmlFilePath) {
        given:
        Reader xml = readTestXml(xmlFilePath).newReader()

        when:
        xmlParser.fromXml(xml)
        xmlParser.setValidationSchema(createValidationSchema())

        then:
        noExceptionThrown()

        where:
        xmlFilePath << ["multi-channel-connections.xml", "default-namespaces.xml", "nested-children.xml"]
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
        String xsdPath = Path.of("schemas", filename).toString()
        StreamSource s = new StreamSource(getClass().getClassLoader().getResourceAsStream(xsdPath))
        s.setSystemId(
                Objects
                        .requireNonNull(getClass().getClassLoader().getResource(xsdPath))
                        .toExternalForm())
        return s
    }

    ComponentRegistry buildRegistryStub() {
        ComponentRegistry registry = Stub() {
            isRegistered(_ as EipId) >> true

            getConnectionType(_ as EipId) >> {
                EipId id ->
                    switch (id) {
                        case new EipId(INTEGRATION.eipNamespace(), "inbound-channel-adapter") ->
                            ConnectionType.SOURCE
                        case new EipId(INTEGRATION.eipNamespace(), "logging-channel-adapter") ->
                            ConnectionType.SINK
                        case new EipId(INTEGRATION.eipNamespace(), "outbound-channel-adapter") ->
                            ConnectionType.SINK
                        case new EipId(INTEGRATION.eipNamespace(), "filter") ->
                            ConnectionType.TEE
                        default -> ConnectionType.PASSTHRU
                    }
            }

            getRole(_ as EipId) >> {
                EipId id ->
                    id == new EipId(INTEGRATION.eipNamespace(), "channel") ?
                            Role.CHANNEL : Role.ENDPOINT
            }
        }

        return registry
    }
}
