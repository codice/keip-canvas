package org.codice.keip.schemas.validation

import com.networknt.schema.AbsoluteIri
import spock.lang.Specification

import static org.codice.keip.schemas.validation.EipSchemaValidator.SCHEMAS_PREFIX

class VersionlessSchemaMapperTest extends Specification {

    def schemaMapper = new EipSchemaValidator.VersionlessSchemaMapper()

    def "IRIs with matching prefix and extension map to schemas on classpath"() {
        given:
        def iri = AbsoluteIri.of(SCHEMAS_PREFIX + "abc.json")
        when:
        def result = schemaMapper.map(iri)
        then:
        result.toString() == "classpath:schemas/abc.json"
    }

    def "unmatched schema IRIs map to null"(String iri) {
        given:
        def absoluteIri = AbsoluteIri.of(iri)
        when:
        def result = schemaMapper.map(absoluteIri)
        then:
        result == null
        where:
        iri << [SCHEMAS_PREFIX + "abc", "abc.json"]
    }
}
