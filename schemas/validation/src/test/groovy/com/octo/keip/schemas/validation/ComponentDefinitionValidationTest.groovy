package org.codice.keip.schemas.validation

import spock.lang.Specification

class ComponentDefinitionValidationTest extends Specification {

    def schemaValidator = EipSchemaValidator.getInstance(EipSchema.COMPONENT_DEFINITION)

    def "Test validation success with valid flow JSON"() {
        given:
        Reader flowJson = readJson("valid/eip-component-def.json")
        when:
        def errors = schemaValidator.validate(flowJson)
        then:
        errors.isEmpty()
    }

    def "Test validation fails with invalid flow JSON"() {
        given:
        Reader flowJson = readJson("invalid/multi-error-eip-component-def.json")
        when:
        def errors = schemaValidator.validate(flowJson)
        then: "validation is 'fail fast' by default"
        errors.size() == 1
    }

    def "Test validation with fails fast disabled -> fails with multiple errors"() {
        given:
        Reader flowJson = readJson("invalid/multi-error-eip-component-def.json")
        when:
        def errors = schemaValidator.validate(flowJson, false)
        then: "validation is 'fail fast' by default"
        errors.size() == 3
    }

    static BufferedReader readJson(String filename) {
        return ComponentDefinitionValidationTest.class.getClassLoader()
                                                .getResource(filename)
                                                .newReader()
    }
}
