package org.codice.keip.schema.xml

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.codice.keip.schema.model.eip.ChildGroup
import org.codice.keip.schema.model.eip.EipComponent
import org.codice.keip.schema.model.eip.EipElement
import org.codice.keip.schema.model.eip.Indicator
import org.codice.keip.schema.model.eip.Occurrence
import org.codice.keip.schema.test.EipComparisonUtils
import org.codice.keip.schema.test.TestIOUtils
import org.apache.ws.commons.schema.XmlSchemaCollection
import spock.lang.Specification

import java.nio.file.Path

import static org.codice.keip.schema.test.TestIOUtils.getXmlSchemaFileReader

class SchemaTranslatorTest extends Specification {

    static final List<EipComponent> EIP_COMPONENTS = importEipComponents(Path.of("translated-eip-components.json"))

    static final Set<String> EXCLUDED_COMPONENTS = ["ignored-component"]

    static final Path SAMPLE_XML = Path.of("translator", "schema-translator-sample.xml")

    def schemaTranslator = new SchemaTranslator(EXCLUDED_COMPONENTS)

    def schemaCollection = new XmlSchemaCollection()

    def "Check end-to-end XML schema to EIP JSON translation success"() {
        given:
        def targetSchema = schemaCollection.read(getXmlSchemaFileReader(SAMPLE_XML))
        schemaCollection.read(getXmlSchemaFileReader(Path.of("imports", "spring-tool.xsd")))

        when:
        List<EipComponent> result = schemaTranslator.translate(schemaCollection, targetSchema)

        then:
        EipComparisonUtils.assertCollectionsEqualNoOrder(
                EIP_COMPONENTS,
                result,
                Comparator.comparing(EipElement::getName),
                EipComparisonUtils::assertEipComponentsEqual,
                "Comparing top level components")
    }

    def "Check EIP translation schema with nested top level child groups (sequence, choice, etc.) not allowed"() {
        given:
        def noOpReducer = Mock(ChildGroupReducer)
        noOpReducer.reduceGroup(_) >> { ChildGroup group -> group }
        schemaTranslator.setGroupReducer(noOpReducer)

        def targetSchema = schemaCollection.read(getXmlSchemaFileReader(SAMPLE_XML))

        when:
        schemaTranslator.translate(schemaCollection, targetSchema)

        then:
        thrown(IllegalArgumentException)
    }

    def "Exception thrown during component translation skips the component"() {
        given:
        def faultyReducer = Mock(ChildGroupReducer)
        faultyReducer.reduceGroup(_) >> { throw new RuntimeException("broken reducer") } >> new ChildGroup(Indicator.SEQUENCE, Occurrence.DEFAULT)
        schemaTranslator.setGroupReducer(faultyReducer)

        def targetSchema = schemaCollection.read(getXmlSchemaFileReader(SAMPLE_XML))

        when:
        List<EipComponent> result = schemaTranslator.translate(schemaCollection, targetSchema)

        then:
        result.size() == 1
        result.getFirst().getName() == "sample-filter"
    }

    private static List<EipComponent> importEipComponents(Path jsonFilePath)
            throws URISyntaxException, IOException {
        Path path = Path.of("schemas", "json").resolve(jsonFilePath)
        String schemaJson = SchemaTranslatorTest.getClassLoader().getResource(path.toString()).text

        Gson gson = TestIOUtils.configureGson()
        def eipComponentListType = new TypeToken<List<EipComponent>>() {}
        return gson.fromJson(schemaJson, eipComponentListType)
    }
}
