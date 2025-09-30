package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.ComponentRegistry
import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.Role
import org.codice.keip.flow.xml.XmlElement
import spock.lang.Specification

import javax.xml.namespace.QName

class DefaultXmlElementTransformerTest extends Specification {

    ComponentRegistry registry = stubRegistry()

    DefaultXmlElementTransformer transformer = new DefaultXmlElementTransformer()

    def "transform XmlElement to EipNode -> success"() {
        given:
        def element = new XmlElement(
                new QName("http://example.com", "adapter", "test"),
                ["id": "n1", "key1": "val1"],
                [])

        when:
        def node = transformer.apply(element, registry)

        then:
        with(node) {
            id() == "n1"
            eipId() == new EipId("test", "adapter")
            role() == Role.ENDPOINT
            connectionType() == ConnectionType.SOURCE
            attributes() == ["key1": "val1"]
            children().isEmpty()
            label() == null
            description() == null
        }
    }

    def "transform XmlElement with nested children to EipNode -> success"() {
        given:
        def child111 = new XmlElement("test2", "child111", ["info": "child111"], [])
        def child11 = new XmlElement("test2", "child11", [:], [child111])
        def child1 = new XmlElement("test", "child1", ["info": "child1", "extra": "val"], [
                child11])

        def child2 = new XmlElement("test", "child2", [:], [])

        def element = new XmlElement(
                new QName("http://example.com", "adapter", "test"),
                ["id": "n1", "key1": "val1"],
                [child1, child2])

        when:
        def node = transformer.apply(element, registry)

        then:
        with(node) {
            id() == "n1"
            eipId() == new EipId("test", "adapter")
            role() == Role.ENDPOINT
            connectionType() == ConnectionType.SOURCE
            attributes() == ["key1": "val1"]
            children().size() == 2
            label() == null
            description() == null
        }

        def c1 = node.children()[0]
        with(c1) {
            eipId() == new EipId("test", "child1")
            attributes() == ["info": "child1", "extra": "val"]
            children().size() == 1
        }

        def c11 = c1.children().getFirst()
        with(c11) {
            eipId() == new EipId("test2", "child11")
            attributes().isEmpty()
            children().size() == 1
        }

        def c111 = c11.children().getFirst()
        with(c111) {
            eipId() == new EipId("test2", "child111")
            attributes() == ["info": "child111"]
            children().isEmpty()
        }

        def c2 = node.children()[1]
        with(c2) {
            eipId() == new EipId("test", "child2")
            attributes().isEmpty()
            children().isEmpty()
        }
    }

    def "transform xml element with no 'id' attribute -> random id is generated"() {
        given:
        def element = new XmlElement(
                new QName("http://example.com", "adapter", "test"),
                ["key1": "val1"],
                [])

        when:
        def node = transformer.apply(element, registry)

        then:
        with(node) {
            id().size() == 10
            eipId() == new EipId("test", "adapter")
            role() == Role.ENDPOINT
            connectionType() == ConnectionType.SOURCE
            attributes() == ["key1": "val1"]
            children().isEmpty()
            label() == null
            description() == null
        }
    }

    def "transform unregistered eip node -> exception thrown"() {
        given:
        def element = new XmlElement(
                new QName("http://example.com", "unknown", "test"),
                ["id": "n1", "key1": "val1"],
                [])

        when:
        transformer.apply(element, registry)

        then:
        thrown(IllegalArgumentException)
    }

    ComponentRegistry stubRegistry() {
        return Stub(ComponentRegistry) {
            isRegistered(_ as EipId) >>
                    { EipId eipId -> return new EipId("test", "adapter") == eipId }

            getConnectionType(_ as EipId) >> Optional.of(ConnectionType.SOURCE)

            getRole(_ as EipId) >> Optional.of(Role.ENDPOINT)

        }
    }
}
