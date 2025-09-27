package org.codice.keip.flow.xml.spring

import spock.lang.Specification

import static org.codice.keip.flow.xml.spring.NodeIdGenerator.DEFAULT_ALPHABET
import static org.codice.keip.flow.xml.spring.NodeIdGenerator.DEFAULT_NUMBER_GENERATOR
import static org.codice.keip.flow.xml.spring.NodeIdGenerator.DEFAULT_SIZE

class NodeIdGeneratorTest extends Specification {

    def "test generating ids"() {
        when:
        def id = NodeIdGenerator.randomId()

        then:
        id.length() == 10
    }

    def "test bad inputs"(Random generator, char[] alphabet, int size) {
        when:
        NodeIdGenerator.randomId(generator, alphabet, size)

        then:
        thrown(IllegalArgumentException)

        where:
        generator                | alphabet         | size
        null                     | DEFAULT_ALPHABET | DEFAULT_SIZE
        DEFAULT_NUMBER_GENERATOR | null             | DEFAULT_SIZE
        DEFAULT_NUMBER_GENERATOR | new char[300]    | DEFAULT_SIZE
        DEFAULT_NUMBER_GENERATOR | DEFAULT_ALPHABET | -1
    }
}
