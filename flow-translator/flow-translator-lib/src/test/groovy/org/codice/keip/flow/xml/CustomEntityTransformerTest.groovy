package org.codice.keip.flow.xml

import com.ctc.wstx.stax.WstxEventFactory
import com.ctc.wstx.stax.WstxOutputFactory
import spock.lang.Specification

import javax.xml.namespace.QName
import javax.xml.stream.XMLEventFactory
import javax.xml.stream.XMLEventWriter

import static org.codice.keip.flow.xml.XmlComparisonUtil.compareXml

class CustomEntityTransformerTest extends Specification {

    private static final XMLEventFactory EVENT_FACTORY = WstxEventFactory.newFactory()

    private static final QName testroot = new QName("testroot")

    def xmlOutput = new StringWriter()

    def xmlWriter = initializeEventWriter()

    def entityTransformer = new CustomEntityTransformer(
            GraphXmlSerializer.initializeXMLInputFactory())

    def "Transform custom entities success"() {
        given:
        def entities = [
                "e1": '<bean class="com.example.Test"><property name="limit" value="65536" /></bean>',
                "e2": '<arbitrary>test</arbitrary>'
        ]

        def expectedXml =
                '<testroot><bean id="e1" class="com.example.Test"><property name="limit" value="65536"/></bean><arbitrary id="e2">test</arbitrary></testroot>'

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), expectedXml)
    }

    def "Transform custom entities existing top-level id is overwritten"() {
        given:
        def entities = ["one": '<arbitrary id="unused">test</arbitrary>']

        def expectedXml =
                '<testroot><arbitrary id="one">test</arbitrary></testroot>'

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), expectedXml)
    }

    def "Transform custom entities with deeply nested elements"() {
        given:
        def entities =
                ["nested": '<top order="one"><second id="2" order="two"><third order="three"/></second></top>']

        def expectedXml =
                '<testroot><top id="nested" order="one"><second id="2" order="two"><third order="three"/></second></top></testroot>'

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.isEmpty()
        compareXml(xmlOutput.toString(), expectedXml)
    }

    def "Transform a custom entity with a top-level comment success"() {
        given:
        def entities = ["one": '<!--root comment--><arbitrary>test</arbitrary>']

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.isEmpty()
        compareXml(
                xmlOutput
                        .toString(), '<testroot><!--root comment--><arbitrary id="one">test</arbitrary></testroot>')
    }

    def "Transform a custom entity with multiple root elements -> error returned"() {
        given:
        def entities = ["one": '<arbitrary>test</arbitrary><other>more</other>']

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.size() == 1
        errors[0].source() == "custom entity [one]"
        compareXml(xmlOutput.toString(), "<testroot/>")
    }

    def "Transform a custom entity with invalid content -> error returned"(String content) {
        given:
        def entities = ["one": content]

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.size() == 1
        errors[0].source() == "custom entity [one]"
        compareXml(xmlOutput.toString(), "<testroot/>")

        where:
        content << invalidXmlExamples()
    }

    def "Transform multiple entities with a single invalid entity -> partial result written and error returned"(String content) {
        given:
        def entities = ["one": content, "two": '<extra role="other"/>']

        when:
        def errors = entityTransformer.apply(entities, xmlWriter)
        closeEventWriter(xmlWriter)

        then:
        errors.size() == 1
        errors[0].source() == "custom entity [one]"
        compareXml(xmlOutput.toString(), '<testroot><extra id="two" role="other"/></testroot>')

        where:
        content << invalidXmlExamples()
    }

    XMLEventWriter initializeEventWriter() {
        def writer = WstxOutputFactory.newFactory().createXMLEventWriter(xmlOutput)
        writer.add(EVENT_FACTORY.createStartElement(testroot, null, null))
        return writer
    }

    void closeEventWriter(XMLEventWriter writer) {
        writer.add(EVENT_FACTORY.createEndElement(testroot, null))
        writer.flush()
        writer.close()
    }

    List<String> invalidXmlExamples() {
        return [
                '',
                'notxml',
                '<unclosed>',
                '<badclose></bclose>',
                '<slashless><slashless',
        ]
    }
}
