package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.ComponentRegistry
import org.codice.keip.flow.xml.NamespaceSpec
import org.springframework.beans.factory.xml.PluggableSchemaResolver
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.xml.sax.InputSource
import spock.lang.Specification

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

import static org.codice.keip.flow.ComponentRegistryIO.readComponentDefinitionJson
import static org.codice.keip.flow.xml.XmlComparisonUtil.readTestXml

// TODO: Test with invalid xml against schema
// TODO: Test with different qname prefixes
class IntegrationGraphXmlParserTest extends Specification {

    private static final List<NamespaceSpec> NAMESPACES = [
            new NamespaceSpec("integration", "http://www.springframework.org/schema/integration", "https://www.springframework.org/schema/integration/spring-integration.xsd"),
            new NamespaceSpec("jms", "http://www.springframework.org/schema/integration/jms", "https://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd")
    ]

    ComponentRegistry componentRegistry = ComponentRegistry.fromJson(readComponentDefinitionJson())

    def xmlParser = new IntegrationGraphXmlParser(NAMESPACES, componentRegistry)

    def "test xsd validation"(String xmlFilePath) {
        given:
        InputStream xml = readTestXml(xmlFilePath)
        xmlParser.setValidationSchema(integrationSchemas())

        when:
        xmlParser.fromXml(xml)

        then:
        noExceptionThrown()

        where:
        xmlFilePath << ["multi-channel-connections.xml", "default-namespaces.xml", "nested-children.xml"]
    }

    private static Schema integrationSchemas() throws Exception {
        PluggableSchemaResolver resolver = new PluggableSchemaResolver(getClass().getClassLoader());

        Set<String> integrationSchemas = discoverIntegrationSchemas();
        List<Source> sources = new ArrayList<>();
        for (String schemaLocation : integrationSchemas) {
            InputSource inputSource = resolver.resolveEntity(null, schemaLocation);
            if (inputSource != null) {
                sources.add(new StreamSource(inputSource.getByteStream()));
            }
        }

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source[] srcArr = sources.toArray(new Source[0]);
        return schemaFactory.newSchema(srcArr);
    }

    private static Set<String> discoverIntegrationSchemas() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:META-INF/spring.schemas");

        List<String> initUrls =
                List.of("http://www.springframework.org/schema/beans/spring-beans.xsd",
                        "https://www.springframework.org/schema/beans/spring-beans.xsd",
                        "http://www.springframework.org/schema/tool/spring-tool.xsd",
                        "https://www.springframework.org/schema/tool/spring-tool.xsd")

        Set<String> schemas = new LinkedHashSet<>(initUrls);
        for (Resource resource : resources) {
            if (!resource.URI.toString().contains("integration")) {
                continue;
            }
            Properties props = new Properties();
            props.load(resource.getInputStream());
            schemas.addAll(props.stringPropertyNames())
        }

        return schemas;
    }
}