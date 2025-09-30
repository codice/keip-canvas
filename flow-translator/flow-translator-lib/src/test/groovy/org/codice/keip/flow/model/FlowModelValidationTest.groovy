package org.codice.keip.flow.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.cfg.EnumFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.codice.keip.schemas.validation.EipSchema
import org.codice.keip.schemas.validation.EipSchemaValidator
import spock.lang.Specification

/**
 * Validates the {@link Flow} POJO against the externally defined EIP JSON schemas:
 * https://github.com/codice/keip-canvas/tree/main/schemas/model/json/eipFlow.schema.json
 */
class FlowModelValidationTest extends Specification {

    private static final JsonMapper MAPPER = createMapper()

    def validator = EipSchemaValidator.getInstance(EipSchema.FLOW)

    def "Fully specified Flow object passes schema validation"() {
        given:
        def child = new EipChild(new EipId("test-ns", "c1"),
                ["cKey1": "cVal1"], [])
        def node = new EipNode("n1",
                new EipId("test-ns", "test-comp"),
                "test-label",
                "test-description",
                Role.TRANSFORMER,
                ConnectionType.PASSTHRU,
                ["key1": "val1"],
                [child])

        def edge = new FlowEdge("e1", "n1", "n2")

        def flow = new Flow([node], [edge])
        when:
        def flowStr = MAPPER.writeValueAsString(flow)
        def errors = validator.validate(new StringReader(flowStr))
        then:
        errors.isEmpty()
    }

    def "Validate ConnectionType Enum values"(ConnectionType connectionType) {
        given:
        def node = new EipNode("n1",
                new EipId("test-ns", "test-comp"),
                null,
                null,
                Role.TRANSFORMER,
                connectionType,
                null,
                null)

        def flow = new Flow([node], [])
        when:
        def flowStr = MAPPER.writeValueAsString(flow)
        def errors = validator.validate(new StringReader(flowStr))
        then:
        errors.isEmpty()
        where:
        connectionType << ConnectionType.values()
    }

    def "Validate Role Enum values"(Role role) {
        given:
        def node = new EipNode("n1",
                new EipId("test-ns", "test-comp"),
                null,
                null,
                role,
                ConnectionType.REQUEST_REPLY,
                null,
                null)

        def flow = new Flow([node], [])
        when:
        def flowStr = MAPPER.writeValueAsString(flow)
        def errors = validator.validate(new StringReader(flowStr))
        then:
        errors.isEmpty()
        where:
        role << Role.values()
    }

    def "Validate EdgeType Enum values"(EdgeProps.EdgeType edgeType) {
        given:
        def node = new EipNode("n1",
                new EipId("test-ns", "test-comp"),
                null,
                null,
                Role.ENDPOINT,
                ConnectionType.REQUEST_REPLY,
                null,
                null)

        def edge = new FlowEdge("e1", "n1", "n2", edgeType)

        def flow = new Flow([node], [edge])
        when:
        def flowStr = MAPPER.writeValueAsString(flow)
        def errors = validator.validate(new StringReader(flowStr))
        then:
        errors.isEmpty()
        where:
        edgeType << EdgeProps.EdgeType.values()
    }

    private static JsonMapper createMapper() {
        JsonMapper mapper = JsonMapper.builder()
                                      .enable(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)
                                      .build()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        return mapper
    }
}
