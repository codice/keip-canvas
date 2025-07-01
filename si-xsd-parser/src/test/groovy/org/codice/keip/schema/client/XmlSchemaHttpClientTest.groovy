package org.codice.keip.schema.client

import org.apache.ws.commons.schema.XmlSchemaCollection
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.io.FileSystemFixture

import java.nio.file.Path

import static org.apache.ws.commons.schema.XmlSchemaSerializer.XSD_NAMESPACE

class XmlSchemaHttpClientTest extends Specification {

    static final TARGET_NAMESPACE = "http://www.example.com/test-target"
    static final FIRST_NAMESPACE = "http://www.example.com/first"
    static final SECOND_NAMESPACE = "http://www.example.com/second"
    static final THIRD_NAMESPACE = "http://www.example.com/third"

    @TempDir
    @Shared
    FileSystemFixture fsFixture

    final URI FILE_URI = fsFixture.resolve("test-target.xsd").toUri()

    Map<String, URI> userProvidedSchemaLocations = [
            (FIRST_NAMESPACE) : fsFixture.resolve("first.xsd").toUri(),
            (SECOND_NAMESPACE): fsFixture.resolve("second.xsd").toUri()
    ]

    def xmlSchemaClient = new XmlSchemaClient(userProvidedSchemaLocations)

    def setupSpec() {
        def XSD_PARENT_PATH = Path.of("/schemas", "xml", "client")
        fsFixture.create {
            copyFromClasspath(XSD_PARENT_PATH.resolve("first.xsd").toString())
            copyFromClasspath(XSD_PARENT_PATH.resolve("second.xsd").toString())
            copyFromClasspath(XSD_PARENT_PATH.resolve("third.xsd").toString())
            copyFromClasspath(XSD_PARENT_PATH.resolve("test-target.xsd").toString())
        }
    }

    def "Collect HTTP/S target and import schemas success"(String uri) {
        when:
        xmlSchemaClient.registerUriResolver(mockUriResolver(), "http", "https")
        def schemaCollection = xmlSchemaClient.collect(URI.create(uri))

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, FIRST_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]

        where:
        uri << ["http://localhost/test-target", "https://localhost/test-target"]
    }

    def "Collect File targets and import schemas success"() {
        when:
        def schemaCollection = xmlSchemaClient.collect(FILE_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, FIRST_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    def "UriResolver fetch URI throws exception"() {
        given:
        def uriResolver = Mock(UriResolver)
        uriResolver.fetchUri(_ as URI) >> { throw new RuntimeException("failure") }
        xmlSchemaClient.registerUriResolver(uriResolver, "http")

        when:
        xmlSchemaClient.collect(URI.create("http://localhost"))

        then:
        thrown(RuntimeException)
    }

    def "Unsupported URI scheme throws exception"() {
        when:
        xmlSchemaClient.collect(URI.create("mailto:localhost"))

        then:
        thrown(RuntimeException)
    }

    def "When an import location is not provided in the user-supplied locations map, ignore the missing URI and collect the rest"() {
        given:
        userProvidedSchemaLocations.remove(FIRST_NAMESPACE)

        when:
        def schemaCollection = xmlSchemaClient.collect(FILE_URI)

        then:
        def namespaces = getNamespaces(schemaCollection)
        namespaces ==~ [TARGET_NAMESPACE, SECOND_NAMESPACE, THIRD_NAMESPACE, XSD_NAMESPACE]
    }

    private UriResolver mockUriResolver() {
        def resolver = Mock(UriResolver)
        resolver.fetchUri(_ as URI) >> fsFixture.resolve("test-target.xsd").toFile().newInputStream()
        return resolver
    }

    private static getNamespaces(XmlSchemaCollection schemaCollection) {
        return schemaCollection.getXmlSchemas().collect { it::getTargetNamespace() }
    }
}
