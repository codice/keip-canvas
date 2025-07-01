package org.codice.keip.schema.config

import org.codice.keip.schema.test.TestIOUtils
import spock.lang.Specification

import java.nio.file.Path

class SchemaRetrievalConfigTest extends Specification {

    def configYaml = TestIOUtils.getYamlConfig(Path.of("test-sample-config.yaml"))

    def "Read schema retrieval config from yaml file"() {
        when:
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
        importedLocations["http://www.example.com/extras"] == URI.create("http://localhost:8080/extras.xsd")
        importedLocations["http://www.example.com/more-extras"] == URI.create("http://localhost:8080/more-extras.xsd")
    }
}
