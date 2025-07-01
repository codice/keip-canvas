package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EipChild
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Role
import spock.lang.Specification

class BaseNodeTransformationTest extends Specification {

    static final String TEST_NS = "test-ns"

    def "single node to xml element"() {
        given:
        def name = "top"
        def node = createNodeStub("1", "top")

        when:
        def element = BaseNodeTransformation.toXmlElement(node)

        then:
        element.prefix() == TEST_NS
        element.localName() == name
        element.attributes().isEmpty()
        element.children().isEmpty()
    }

    def "single node with attributes and children to xml element"() {
        given:
        def grandchildAttrs = ["deepest-attr": "val"]
        def grandchild = new EipChild("grandchild", grandchildAttrs, null)

        def childAttrs = ["child-attr1": "4"]
        def child = new EipChild("child", childAttrs, [grandchild])

        def name = "top"
        def topAttrs = ["attr1": "123", "attr2": "abc"]
        def node = createNodeStub("1", name)
        node.attributes() >> topAttrs
        node.children() >> [child]

        when:
        def element = BaseNodeTransformation.toXmlElement(node)

        then:
        element.prefix() == TEST_NS
        element.localName() == name
        element.attributes() == topAttrs
        element.children().size() == 1

        def aChild = element.children()[0]
        aChild.prefix() == TEST_NS
        aChild.localName() == child.name()
        aChild.attributes() == childAttrs
        aChild.children().size() == 1

        def aGrandchild = aChild.children()[0]
        aGrandchild.prefix() == TEST_NS
        aGrandchild.localName() == grandchild.name()
        aGrandchild.attributes() == grandchildAttrs
        aGrandchild.children().isEmpty()
    }

    def "single node with multiple children to xml element"() {
        given:
        def childAttrs1 = ["attr1": "one"]
        def child1 = new EipChild("child1", childAttrs1, null)

        def child2 = new EipChild("child2", null, null)

        def name = "top"
        def node = createNodeStub("1", name)
        node.children() >> [child1, child2]

        when:
        def element = BaseNodeTransformation.toXmlElement(node)

        then:
        element.prefix() == TEST_NS
        element.localName() == name
        element.attributes().isEmpty()
        element.children().size() == 2

        def aChild1 = element.children()[0]
        aChild1.prefix() == TEST_NS
        aChild1.localName() == child1.name()
        aChild1.attributes() == childAttrs1
        aChild1.children().isEmpty()

        def aChild2 = element.children()[1]
        aChild2.prefix() == TEST_NS
        aChild2.localName() == child2.name()
        aChild2.attributes().isEmpty()
        aChild2.children().isEmpty()
    }

    EipNode createNodeStub(String nodeId, String eipName) {
        EipNode stub = Stub {
            id() >> nodeId
            eipId() >> new EipId(TEST_NS, eipName)
            role() >> Role.TRANSFORMER
            connectionType() >> ConnectionType.PASSTHRU
        }
        return stub
    }
}
