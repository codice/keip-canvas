package org.codice.keip.xsd.model.serdes

import com.google.gson.JsonParser
import org.codice.keip.xsd.model.eip.Attribute
import org.codice.keip.xsd.model.eip.AttributeType
import org.codice.keip.xsd.model.eip.ChildGroup
import org.codice.keip.xsd.model.eip.ConnectionType
import org.codice.keip.xsd.model.eip.EipChildElement
import org.codice.keip.xsd.model.eip.EipComponent
import org.codice.keip.xsd.model.eip.EipSchema
import org.codice.keip.xsd.model.eip.Indicator
import org.codice.keip.xsd.model.eip.Occurrence
import org.codice.keip.xsd.model.eip.Restriction
import org.codice.keip.xsd.model.eip.Role
import org.codice.keip.schemas.validation.EipSchemaValidator
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import static org.codice.keip.schemas.validation.EipSchema.COMPONENT_DEFINITION

class SchemaSerializerTest extends Specification {

    static final String EXPECTED_SCHEMA_JSON = importEipSchemaJson()

    @TempDir
    File testDir

    def "Serialize EipSchema to JSON String success"() {
        given:
        def eipSchema = buildTestSchema()
        def outputFile = new File(testDir, "schema.json")

        when:
        SchemaSerializer.writeSchemaToJsonFile(eipSchema, outputFile)

        then:
        JsonParser.parseString(outputFile.text) == JsonParser.parseString(EXPECTED_SCHEMA_JSON)
    }

    def "Serialize EipSchema to JSON String - missing parent directories are created automatically"() {
        given:
        def eipSchema = buildTestSchema()
        def outputFile = Path.of(testDir.path, "first", "second", "schema.json").toFile()

        when:
        SchemaSerializer.writeSchemaToJsonFile(eipSchema, outputFile)

        then:
        JsonParser.parseString(outputFile.text) == JsonParser.parseString(EXPECTED_SCHEMA_JSON)
    }

    def "Validate serialized output against the EipComponentDefinition schema"() {
        given:
        def eipSchema = buildTestSchema()
        def outputFile = new File(testDir, "schema.json")

        def validator = EipSchemaValidator.getInstance(COMPONENT_DEFINITION)

        when:
        SchemaSerializer.writeSchemaToJsonFile(eipSchema, outputFile)
        def errors = validator.validate(outputFile.newReader(), false)

        then:
        errors.isEmpty()
    }

    def "Test custom occurrence deserializer"(Occurrence input, String expectedJson) {
        when:
        def result = SchemaSerializer.GSON.toJson(input)

        then:
        JsonParser.parseString(result) == JsonParser.parseString(expectedJson)

        where:
        input                                   | expectedJson
        Occurrence.DEFAULT                      | ""
        new Occurrence(0, 1)                    | "{min: 0}"
        new Occurrence(1, 3)                    | "{max: 3}"
        new Occurrence(5, 10)                   | "{min: 5, max: 10}"
        new Occurrence(0, Occurrence.UNBOUNDED) | "{min: 0, max: -1}"
    }

    private static String importEipSchemaJson() throws URISyntaxException, IOException {
        Path path = Path.of("schemas", "json", "serialization-sanity-check.json")
        return SchemaSerializerTest.getClassLoader().getResource(path.toString()).text
    }

    private static EipSchema buildTestSchema() {
        def attr = new Attribute.Builder("test-attr", AttributeType.STRING)
                .description("test attr description")
                .defaultValue("default val")
                .required(false)
                .restriction(new Restriction.MultiValuedRestriction(
                        Restriction.RestrictionType.ENUM, List.of("first", "second")))
                .build()
        def child = new EipChildElement.Builder("test-child")
                .description("test child description").build()
        def eipComponent = new EipComponent.Builder("test-top",
                Role.ENDPOINT, ConnectionType.SOURCE)
                .description("test top level description")
                .addAttribute(attr)
                .childGroup(new ChildGroup(Indicator.SEQUENCE, List.of(child)))
                .build()
        return EipSchema.from(Map.of("test-ns", List.of(eipComponent)))
    }

}
