package org.codice.keip.flow.xml

import org.codice.keip.flow.model.EipId
import spock.lang.Specification

class NodeTransformerFactoryTest extends Specification {

    def "Test with default transformer only"() {
        given:
        NodeTransformer defaultTransformer = Stub()

        when:
        def factory = new NodeTransformerFactory(defaultTransformer)

        then:
        def transformer = factory.getNodeTransformer(new EipId("test", "one"))
        transformer.is(defaultTransformer)
    }

    def "Test with registered custom transformers"() {
        given:
        NodeTransformer defaultTransformer = Stub()
        NodeTransformer customTransformer = Stub()

        when:
        def factory = new NodeTransformerFactory(defaultTransformer)
        factory.registerNodeTransformer(new EipId("custom", "comp"), customTransformer)

        then:
        def first = factory.getNodeTransformer(new EipId("test", "one"))
        first.is(defaultTransformer)

        def second = factory.getNodeTransformer(new EipId("custom", "comp"))
        second.is(customTransformer)
    }
}
