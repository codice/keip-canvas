package org.codice.keip.xsd.xml

import org.apache.ws.commons.schema.XmlSchemaCollection
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.walker.XmlSchemaWalker
import org.codice.keip.xsd.model.eip.Attribute
import org.codice.keip.xsd.model.eip.AttributeType
import org.codice.keip.xsd.model.eip.ChildGroup
import org.codice.keip.xsd.model.eip.ConnectionType
import org.codice.keip.xsd.model.eip.EipChildElement
import org.codice.keip.xsd.model.eip.EipId
import org.codice.keip.xsd.model.eip.Indicator
import org.codice.keip.xsd.model.eip.Occurrence
import org.codice.keip.xsd.model.eip.Role
import org.codice.keip.xsd.test.TestIOUtils
import spock.lang.Specification

import java.nio.file.Path

class EipTranslationVisitorTest extends Specification {

    private static final String EIP_NS = "testNs"

    private static final Map<String, String> NAMESPACE_MAP = ["test-ns": EIP_NS]

    static final TEST_XML_NAMESPACE = "test-ns"

    def xmlSchemaCollection = new XmlSchemaCollection()

    def visitor = new EipTranslationVisitor(NAMESPACE_MAP)

    XmlSchemaWalker walker =
            setupWalker(xmlSchemaCollection, Path.of("visitor", "eip-visitor-sample.xml"))

    def "Top level element is set as the main EipComponent"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))
        def eipComponent = visitor.getEipComponent()
        def reduceGroup = new ChildGroupReducer().reduceGroup(eipComponent.getChildGroup())

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, "top-level-component")
        eipComponent.getDescription() == "Top Level EIP Component"
        eipComponent.getRole() == Role.ENDPOINT
        eipComponent.getConnectionType() == ConnectionType.PASSTHRU

        def expectedAttrs = [new Attribute.Builder("top-attr-1", AttributeType.NUMBER)
                                     .defaultValue("1")
                                     .build(),
                             new Attribute.Builder("top-attr-2", AttributeType.STRING)
                                     .required(true)
                                     .build()] as HashSet
        eipComponent.attributes == expectedAttrs
    }

    def "Visit multiple top level elements without resetting visitor throws exception"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))

        then:
        thrown(IllegalStateException)
    }

    def "Visit element with an unknown namespace URI throws exception"() {
        given:
        def translationVisitor = new EipTranslationVisitor([:])
        def collection = new XmlSchemaCollection()
        XmlSchemaWalker schemaWalker =
                setupWalker(
                        translationVisitor,
                        collection, Path.of("visitor", "eip-visitor-sample.xml"))

        when:
        schemaWalker.walk(getTopLevelComponent(collection, "top-level-component"))

        then:
        thrown(IllegalArgumentException)
    }

    def "Top level element test single ChildGroup"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        def group = eipComponent.getChildGroup() as ChildGroup
        group.indicator() == Indicator.SEQUENCE
        group.occurrence() == new Occurrence(0, Occurrence.UNBOUNDED)
        group.children().size() == 1
    }

    def "Visit with previously unseen child element"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, "top-level-component")
        def group = eipComponent.getChildGroup() as ChildGroup
        group.children().size() == 1

        def childElement = group.children().getFirst() as EipChildElement
        childElement.getEipId() == new EipId(EIP_NS, "childElement1")
        childElement.occurrence() == new Occurrence(2, 4)
        childElement.getDescription() == "baseType example docs"

        def expectedAttrs = [new Attribute.Builder("child-attr-1", AttributeType.BOOLEAN)
                                     .description("Enable thing")
                                     .build()] as HashSet
        childElement.attributes == expectedAttrs
    }

    def "Visit with previously visited child element"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))
        visitor.reset()
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "alt-top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, "alt-top-level-component")
        def group = eipComponent.getChildGroup() as ChildGroup
        group.occurrence() == new Occurrence(0, Occurrence.UNBOUNDED)
        group.children().size() == 2

        def childElement1 = group.children().getFirst() as EipChildElement
        childElement1.getEipId() == new EipId(EIP_NS, "altChildElement1")
        childElement1.occurrence() == Occurrence.DEFAULT
        childElement1.getDescription() == "baseType example docs"

        def childElement2 = group.children().getLast() as EipChildElement
        childElement2.getEipId() == new EipId(EIP_NS, "altChildElement2")
        childElement2.occurrence() == Occurrence.DEFAULT
        childElement2.getDescription() == "override base docs"

        def expectedAttrs = [new Attribute.Builder("child-attr-1", AttributeType.BOOLEAN)
                                     .description("Enable thing")
                                     .build()] as HashSet
        childElement1.attributes == expectedAttrs
        childElement2.attributes == expectedAttrs
    }

    def "Circular references (non-top-level) are handled with back-references in EipSchema"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "test-circular-refs"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, "test-circular-refs")
        def recursiveParent = eipComponent.getChildGroup().children().getFirst() as EipChildElement
        recursiveParent.getEipId() == new EipId(EIP_NS, "recursive-parent-element")

        def recursiveChild = recursiveParent.children().getFirst() as EipChildElement
        recursiveChild.getEipId() == new EipId(EIP_NS, "recursive-child-element")

        def backRef = recursiveChild.children().getFirst() as EipChildElement
        backRef.is(recursiveParent)
    }

    def "Circular reference to a top-level component is not supported"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "recursive-parent-element"))

        then:
        thrown(NullPointerException)
    }

    def "Visit handle nested children"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "top-level-component"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, "top-level-component")
        def group = eipComponent.getChildGroup() as ChildGroup
        group.children().size() == 1

        def childElement = group.children().getFirst() as EipChildElement
        childElement.getEipId() == new EipId(EIP_NS, "childElement1")

        def nestedGroup = childElement.getChildGroup() as ChildGroup
        nestedGroup.indicator() == Indicator.SEQUENCE
        nestedGroup.occurrence() == Occurrence.DEFAULT

        nestedGroup.children().size() == 2

        def firstGroup = nestedGroup.children().getFirst() as ChildGroup
        firstGroup.indicator() == Indicator.CHOICE
        firstGroup.occurrence() == new Occurrence(1, 3)
        firstGroup.children().size() == 1
        (firstGroup.children().getFirst() as EipChildElement).getEipId() == new EipId(
                EIP_NS, "nestedChild1")

        def secondGroup = nestedGroup.children().getLast() as ChildGroup
        secondGroup.indicator() == Indicator.SEQUENCE
        secondGroup.occurrence() == new Occurrence(0, 1)
        secondGroup.children().size() == 1
        (secondGroup.children().getFirst() as EipChildElement).getEipId() == new EipId(
                EIP_NS, "nestedChild2")
    }

    def "Visit component with an ALL group"() {
        when:
        walker.walk(getTopLevelComponent(xmlSchemaCollection, "component-with-all-group"))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, "component-with-all-group")
        def group = eipComponent.getChildGroup() as ChildGroup
        group.indicator() == Indicator.ALL
        group.children().size() == 2
    }

    def "EIP Component check connection type is set correctly"(String elementName, ConnectionType expectedType) {
        given:
        def schemaCollection = new XmlSchemaCollection()
        def localWalker =
                setupWalker(schemaCollection, Path.of("visitor", "connection-type-test-input.xml"))

        when:
        localWalker.walk(getTopLevelComponent(schemaCollection, elementName))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, elementName)
        eipComponent.getConnectionType() == expectedType

        where:
        elementName                   | expectedType
        "requestReplyElement"         | ConnectionType.REQUEST_REPLY
        "outboundGateway"             | ConnectionType.REQUEST_REPLY
        "outbound-gateway-with-attrs" | ConnectionType.REQUEST_REPLY
        "test-inbound-gateway"        | ConnectionType.INBOUND_REQUEST_REPLY
        "tee-type-element"            | ConnectionType.TEE
        "validating-Filter"           | ConnectionType.TEE
        "example-Router"              | ConnectionType.CONTENT_BASED_ROUTER
        "InboundElement"              | ConnectionType.SOURCE
        "source"                      | ConnectionType.SOURCE
        "example-message-driven"      | ConnectionType.SOURCE
        "example-Outbound"            | ConnectionType.SINK
        "sink"                        | ConnectionType.SINK
        "handler"                     | ConnectionType.PASSTHRU
    }

    def "Eip Component check role is set correctly"(String elementName, Role expectedRole) {
        given:
        def schemaCollection = new XmlSchemaCollection()
        def localWalker = setupWalker(
                schemaCollection, Path.of("visitor", "role-test-input.xml"))

        when:
        localWalker.walk(getTopLevelComponent(schemaCollection, elementName))
        def eipComponent = visitor.getEipComponent()

        then:
        eipComponent.getEipId() == new EipId(EIP_NS, elementName)
        eipComponent.getRole() == expectedRole

        where:
        elementName          | expectedRole
        "connector"          | Role.CHANNEL
        "validating-Filter"  | Role.ROUTER
        "dynamicRouter"      | Role.ROUTER
        "messageTransformer" | Role.TRANSFORMER
        "handler"            | Role.ENDPOINT
    }

    def "Visit multiple nodes with the same child names but different types"() {
        given:
        def schemaCollection = new XmlSchemaCollection()
        def localWalker = setupWalker(
                schemaCollection, Path.of("visitor", "element-name-overlap.xml"))
        when:
        localWalker.walk(getTopLevelComponent(schemaCollection, "type-router"))
        def typeRouter = visitor.getEipComponent()
        visitor.reset()
        localWalker.walk(getTopLevelComponent(schemaCollection, "basic-router"))
        def basicRouter = visitor.getEipComponent()
        visitor.reset()
        localWalker.walk(getTopLevelComponent(schemaCollection, "value-router"))
        def valueRouter = visitor.getEipComponent()

        then:
        def typeRouterChild = typeRouter.getChildGroup().children()[0] as EipChildElement
        typeRouterChild.getEipId() == new EipId(EIP_NS, "mapping")
        typeRouterChild.getAttributes()[0].name() == "type"

        def basicRouterChild = basicRouter.getChildGroup().children()[0] as EipChildElement
        basicRouterChild.getEipId() == new EipId(EIP_NS, "mapping")
        basicRouterChild.getAttributes()[0].name() == "value"

        def valueRouterChild = valueRouter.getChildGroup().children()[0] as EipChildElement
        valueRouterChild.getEipId() == new EipId(EIP_NS, "mapping")
        valueRouterChild.getAttributes()[0].name() == "value"
    }

    private XmlSchemaWalker setupWalker(XmlSchemaCollection schemaCollection, Path xmlFilePath) {
        return setupWalker(this.visitor, schemaCollection, xmlFilePath)
    }

    private static XmlSchemaWalker setupWalker(EipTranslationVisitor eipVisitor, XmlSchemaCollection schemaCollection, Path xmlFilePath) {
        schemaCollection.read(TestIOUtils.getXmlSchemaFileReader(xmlFilePath))
        def walker = new XmlSchemaWalker(schemaCollection)
        walker.addVisitor(eipVisitor)
        return walker
    }

    private static XmlSchemaElement getTopLevelComponent(XmlSchemaCollection schemaCollection, String name) {
        return schemaCollection.schemaForNamespace(TEST_XML_NAMESPACE).getElementByName(name)
    }
}