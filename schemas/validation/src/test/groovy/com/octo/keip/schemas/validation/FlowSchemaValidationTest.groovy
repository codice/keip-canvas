package org.codice.keip.schemas.validation

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class FlowSchemaValidationTest extends Specification {

    def schemaValidator = EipSchemaValidator.getInstance(EipSchema.FLOW)

    def "Test validation success with valid flow JSON"() {
        given:
        Reader flowJson = readJson("valid/eip-flow.json")
        when:
        def errors = schemaValidator.validate(flowJson)
        then:
        errors.isEmpty()
    }

    def "Test validation fails with invalid flow JSON"() {
        given:
        Reader flowJson = readJson("invalid/multi-error-eip-flow.json")
        when:
        def errors = schemaValidator.validate(flowJson)
        then: "validation is 'fail fast' by default"
        errors.size() == 1
    }

    def "Test validation with fails fast disabled -> fails with multiple errors"() {
        given:
        Reader flowJson = readJson("invalid/multi-error-eip-flow.json")
        when:
        def errors = schemaValidator.validate(flowJson, false)
        then: "validation is 'fail fast' by default"
        errors.size() == 3
    }

    def "JsonMapper throws IOError -> IOError is rethrown as RuntimeException"() {
        given:
        Reader flowJson = readJson("valid/eip-flow.json")
        ObjectMapper mapper = Stub() {
            readTree(_ as Reader) >> { throw new IOException("io error") }
        }
        schemaValidator.setJsonMapper(mapper)
        when:
        schemaValidator.validate(flowJson)
        then:
        thrown(RuntimeException)
    }

    static BufferedReader readJson(String filename) {
        return FlowSchemaValidationTest.class.getClassLoader()
                                       .getResource(filename)
                                       .newReader()
    }
}
