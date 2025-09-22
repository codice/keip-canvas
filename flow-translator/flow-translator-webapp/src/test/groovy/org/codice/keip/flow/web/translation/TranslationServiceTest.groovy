package org.codice.keip.flow.web.translation

import org.codice.keip.flow.FlowTranslator
import org.codice.keip.flow.error.TransformationError
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Flow
import spock.lang.Specification

import javax.xml.transform.TransformerException
import java.nio.file.Path

class TranslationServiceTest extends Specification {

    private static final String OUTPUT_XML = "<test>canned</test>"

    Flow outputFlow = buildOutputFlow()

    FlowTranslator flowTranslator = Stub()

    def translationSvc = new TranslationService(flowTranslator)

    def "transform flow to xml error-free -> transformed data plus null error field"() {
        given:
        flowTranslator.toXml(_ as Flow, _ as Writer) >> {
            args ->
                {
                    Writer w = args[1]
                    w.write(OUTPUT_XML)
                    return []
                }
        }

        when:
        def response = translationSvc.toXml(new Flow([], []))

        then:
        response == new TranslationResponse(OUTPUT_XML, null)
    }

    def "transform flow to xml with non-critical errors -> transformed partial data plus detailed error field"() {
        given:
        flowTranslator.toXml(_ as Flow, _ as Writer) >> {
            args ->
                {
                    Writer w = args[1]
                    w.write(OUTPUT_XML)
                    return [new TransformationError("node1", new TransformerException("oops"))]
                }
        }

        when:
        def response = translationSvc.toXml(new Flow([], []))

        then:
        with(response) {
            data() == OUTPUT_XML
            error() != null
            error().details().size() == 1
        }
    }

    def "transform flow to xml with critical error -> throw runtime exception"() {
        given:
        flowTranslator.toXml(_ as Flow, _ as Writer) >> { throw new TransformerException("oops") }

        when:
        translationSvc.toXml(new Flow([], []))

        then:
        thrown(RuntimeException)
    }

    def "transform flow to xml with pretty print -> transformed data is formatted, no errors"() {
        given:
        flowTranslator.toXml(_ as Flow, _ as Writer) >> {
            args ->
                {
                    Writer w = args[1]
                    w.write(readXml("sample-integration-route.xml"))
                    return []
                }
        }

        when:
        def response = translationSvc.toXml(new Flow([], []), true)

        then:
        response == new TranslationResponse(readXml("formatted-sample.xml"), null)
    }

    def "transform xml to flow error-free -> transformed data plus null error field"() {
        given:
        flowTranslator.fromXml(_ as InputStream) >> outputFlow

        when:
        def is = new ByteArrayInputStream("".getBytes())
        def response = translationSvc.fromXml(is)

        then:
        response == new TranslationResponse(outputFlow, null)
    }

    def "transform xml to flow with error -> throw runtime exception"() {
        given:
        flowTranslator.fromXml(_ as InputStream) >> { throw new TransformerException("oops") }

        when:
        def is = new ByteArrayInputStream("".getBytes())
        translationSvc.fromXml(is)

        then:
        thrown(RuntimeException)
    }

    private Flow buildOutputFlow() {
        EipNode test = Stub() {
            id() >> "test"
        }
        return new Flow([test], [])
    }

    private static String readXml(String filename) {
        Path path = Path.of("xml").resolve(filename)
        return TranslationServiceTest.class.getClassLoader()
                                     .getResource(path.toString()).text
    }
}
