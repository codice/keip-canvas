package org.codice.keip.xsd.config

import org.codice.keip.xsd.test.TestIOUtils
import spock.lang.Specification

import java.nio.file.Path

class SchemaRetrievalConfigTest extends Specification {

    def "Read schema retrieval config from yaml file"() {
        when:
        def configYaml = TestIOUtils.getYamlConfig(Path.of("test-sample-config.yaml"))
        def config = XsdSourceConfiguration.readYaml(configYaml)

        then:
        config.getSchemas().size() == 2

        def first = config.getSchemas()[0]
        first.getAlias() == "first"
        first.getNamespace() == "http://www.example.com/first"
        first.getLocation() == URI.create("http://localhost:8080/first.xsd")
        first.getExcludedElements() == ["excluded1", "excluded2"] as Set

        def second = config.getSchemas()[1]
        second.getAlias() == "second"
        second.getNamespace() == "http://www.example.com/second"
        second.getLocation() == URI.create("http://localhost:8080/second.xsd")
        second.getExcludedElements() == [] as Set

        def importedLocations = config.getImportedSchemaLocationsMap()
        importedLocations.size() == 2
        importedLocations["http://www.example.com/extras"] ==
                URI.create("http://localhost:8080/extras.xsd")
        importedLocations["http://www.example.com/more-extras"] ==
                URI.create("http://localhost:8080/more-extras.xsd")
    }

    def "Read schema retrieval config with missing importedSchemaLocations field"() {
        when:
        def configYaml = TestIOUtils.getYamlConfig(Path.of("end-to-end-config.yaml"))
        def config = XsdSourceConfiguration.readYaml(configYaml)

        then:
        config.getSchemas().size() == 2
        config.getImportedSchemaLocationsMap().isEmpty()
    }

    def "Read schema retrieval config with missing schemas field throws exception"(String yaml) {
        when:
        def configYaml = new ByteArrayInputStream(yaml.getBytes())
        XsdSourceConfiguration.readYaml(configYaml)

        then:
        thrown(IllegalArgumentException)

        where:
        yaml << ["", "schemas:"]
    }
}
