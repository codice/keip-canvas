package org.codice.keip.schema.xml.attribute

import org.apache.ws.commons.schema.XmlSchemaAnnotation
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem
import org.apache.ws.commons.schema.XmlSchemaAppInfo
import org.apache.ws.commons.schema.XmlSchemaAttribute
import org.apache.ws.commons.schema.XmlSchemaDocumentation
import org.apache.ws.commons.schema.XmlSchemaElement
import org.apache.ws.commons.schema.XmlSchemaSimpleType
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import spock.lang.Specification

class AnnotationTranslatorTest extends Specification {

    def translator = new AnnotationTranslator();

    def "Get description from XmlSchemaElement with null Annotation and SchemaType returns null"() {
        given:
        def element = new XmlSchemaElement(null, false)

        when:
        def description = translator.getDescription(element)

        then:
        description == null
    }

    def "Get description from XmlSchemaElement with null SchemaType annotation returns null"() {
        given:
        def element = new XmlSchemaElement(null, false)
        element.setSchemaType(new XmlSchemaSimpleType(null, false))

        when:
        def description = translator.getDescription(element)

        then:
        description == null
    }

    def "Get description from XmlSchemaAttribute with null annotation returns null"() {
        given:
        def xmlAttr = new XmlSchemaAttribute(null, false)

        when:
        def description = translator.getDescription(xmlAttr)

        then:
        description == null
    }

    def "Get description from null XmlSchemaAnnotation returns null"() {
        given:
        XmlSchemaAnnotation xmlAnnotation = null

        expect:
        translator.getDescription(xmlAnnotation) == null
    }

    def "Get description from XmlSchemaAnnotation with empty items returns null"() {
        given:
        XmlSchemaAnnotation emptyItems = new XmlSchemaAnnotation()

        expect:
        translator.getDescription(emptyItems) == null

    }

    def "Get description from single XmlSchemaDocumentation annotation item"(List<String> annotation, String expectedDescription) {
        given:
        def xmlAnnotation = createDocumentationAnnotation(*annotation)

        when:
        def description = translator.getDescription(xmlAnnotation)

        then:
        description == expectedDescription

        where:
        annotation                          | expectedDescription
        ["first second"]                    | "first second"
        ["\nfirst\nsecond"]                 | "first second"
        ["   first\n   second   third. \n"] | "first second third."
        ["first\tsecond"]                   | "first second"
        ["first 'second' \"third\""]        | "first 'second' \"third\""
        ["first", "second", "third"]        | "first second third"
        ["first\n", "second\t"]             | "first second"
    }

    def "Get description from single xmlSchemaAppInfo annotation item"(List<String> annotation, String expectedDescription) {
        given:
        def xmlAnnotation = createAppInfoAnnotation(*annotation)

        when:
        def description = translator.getDescription(xmlAnnotation)

        then:
        description == expectedDescription

        where:
        annotation                          | expectedDescription
        ["first second"]                    | "first second"
        ["\nfirst\nsecond"]                 | "first second"
        ["   first\n   second   third. \n"] | "first second third."
        ["first\tsecond"]                   | "first second"
        ["first 'second' \"third\""]        | "first 'second' \"third\""
        ["first", "second", "third"]        | "first second third"
        ["first\n", "second\t"]             | "first second"
    }

    def "Get description from multiple annotation items"() {
        given:
        def item1 = new XmlSchemaDocumentation()
        item1.setMarkup(mockNodeList(""))
        def item2 = new XmlSchemaAppInfo()
        item2.setMarkup(mockNodeList("second appinfo"))
        def item3 = new XmlSchemaDocumentation()
        item3.setMarkup(mockNodeList("third doc"))

        def xmlAnnotation = new XmlSchemaAnnotation()
        xmlAnnotation.getItems().addAll(item1, item2, item3)

        when:
        def description = translator.getDescription(xmlAnnotation)

        then:
        description == "second appinfo third doc"
    }

    def "Get description with empty NodeList content returns null"() {
        given:
        def xmlAnnotation = createDocumentationAnnotation()

        when:
        def description = translator.getDescription(xmlAnnotation)

        then:
        description == null
    }

    def "Get description with null markup returns null"() {
        given:
        def doc = new XmlSchemaDocumentation()
        def xmlAnnotation = new XmlSchemaAnnotation()
        xmlAnnotation.getItems().add(doc)

        when:
        def description = translator.getDescription(xmlAnnotation)

        then:
        description == null
    }

    def "Get description with empty or blank markup content returns null"(String annotation) {
        given:
        def xmlAnnotation = createDocumentationAnnotation(annotation)

        when:
        def description = translator.getDescription(xmlAnnotation)

        then:
        description == null

        where:
        annotation << ["", " ", "\n"]
    }

    def "Get description with unknown XmlSchemaAnnotationItem subclass"() {
        given:
        def item = new XmlSchemaAnnotationItem() {}
        def xmlAnnotation = new XmlSchemaAnnotation()
        xmlAnnotation.getItems().add(item)

        when:
        translator.getDescription(xmlAnnotation)

        then:
        thrown(IllegalStateException)
    }

    private XmlSchemaAnnotation createDocumentationAnnotation(String... annotations) {
        def annotationItem = new XmlSchemaDocumentation()
        annotationItem.setMarkup(mockNodeList(annotations))
        def xmlAnnotation = new XmlSchemaAnnotation()
        xmlAnnotation.getItems().add(annotationItem)
        return xmlAnnotation
    }

    private XmlSchemaAnnotation createAppInfoAnnotation(String... annotations) {
        def annotationItem = new XmlSchemaAppInfo()
        annotationItem.setMarkup(mockNodeList(annotations))
        def xmlAnnotation = new XmlSchemaAnnotation()
        xmlAnnotation.getItems().add(annotationItem)
        return xmlAnnotation
    }

    private NodeList mockNodeList(String... contentStrings) {

        List<Node> nodes = contentStrings.collect {
            {
                def node = Mock(Node)
                node.getTextContent() >> it
                return node
            }
        }

        def nodeList = Mock(NodeList)
        nodeList.getLength() >> nodes.size()
        nodeList.item(_) >> { int index -> nodes[index] }
        return nodeList
    }
}
