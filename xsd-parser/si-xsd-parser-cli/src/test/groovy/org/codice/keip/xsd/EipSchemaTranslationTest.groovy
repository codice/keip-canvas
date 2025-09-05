package org.codice.keip.xsd

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.ws.commons.schema.XmlSchemaCollection
import org.codice.keip.xsd.client.XmlSchemaClient
import org.codice.keip.xsd.config.XsdSourceConfiguration
import org.codice.keip.xsd.model.eip.EipComponent
import org.codice.keip.xsd.model.eip.EipElement
import org.codice.keip.xsd.model.eip.EipSchema
import org.codice.keip.xsd.test.EipComparisonUtils
import org.codice.keip.xsd.test.TestIOUtils
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class EipSchemaTranslationTest extends Specification {

    static final URI ns1Location = URI.create("http://localhost:8080/ns1.xsd")
    static final URI ns2Location = URI.create("http://localhost:8080/ns2.xsd")

    @Shared
            expectedEipMap = importEipSchema(Path.of("end-to-end-eip-schema.json")).toMap()

    @Shared
            schemaSourceConfig = XsdSourceConfiguration.readYaml(TestIOUtils.getYamlConfig(Path.of("end-to-end-config.yaml")))

    def "Use XmlSchemaSourceConfiguration to translate XML to EIP schema"() {
        given:
        def schemaCollectionFirst = new XmlSchemaCollection()
        schemaCollectionFirst.read(TestIOUtils.getXmlSchemaFileReader(Path.of("end-to-end", "ns1.xml")))

        def schemaCollectionSecond = new XmlSchemaCollection()
        schemaCollectionSecond.read(TestIOUtils.getXmlSchemaFileReader(Path.of("end-to-end", "ns2.xml")))

        def schemaClient = Mock(XmlSchemaClient)
        schemaClient.collect(_ as URI) >> { args -> args[0] == ns1Location ? schemaCollectionFirst : schemaCollectionSecond }

        when:
        def eipTranslation = new EipSchemaTranslation(schemaSourceConfig, schemaClient)
        def resultMap = eipTranslation.getEipSchema().toMap();

        then:
        resultMap.size() == 2
        eipTranslation.getErrors().isEmpty()
        EipComparisonUtils.assertCollectionsEqualNoOrder(resultMap["ns1"], expectedEipMap["ns1"], Comparator.comparing(EipElement::getName), EipComparisonUtils::assertEipComponentsEqual, "Comparing ns1 components")
        EipComparisonUtils.assertCollectionsEqualNoOrder(resultMap["ns2"], expectedEipMap["ns2"], Comparator.comparing(EipElement::getName), EipComparisonUtils::assertEipComponentsEqual, "Comparing ns2 components")
    }

    def "If translating a schema throws an exception, move on to next one"() {
        given:
        def schemaCollectionSecond = new XmlSchemaCollection()
        schemaCollectionSecond.read(TestIOUtils.getXmlSchemaFileReader(Path.of("end-to-end", "ns2.xml")))

        def schemaClient = Mock(XmlSchemaClient)
        schemaClient.collect(_ as URI) >> { args ->
            {
                if (args[0] == ns2Location) {
                    return schemaCollectionSecond
                }
                throw new RuntimeException("schema fail")
            }
        }

        when:
        def eipTranslation = new EipSchemaTranslation(schemaSourceConfig, schemaClient)
        def resultMap = eipTranslation.getEipSchema().toMap();

        then:
        resultMap.size() == 1
        eipTranslation.getErrors().size() == 1
        EipComparisonUtils.assertCollectionsEqualNoOrder(resultMap["ns2"], expectedEipMap["ns2"], Comparator.comparing(EipElement::getName), EipComparisonUtils::assertEipComponentsEqual, "Comparing ns2 components")
    }

    private static EipSchema importEipSchema(Path jsonFilePath) throws URISyntaxException, IOException {
        Path path = Path.of("schemas", "json").resolve(jsonFilePath)
        String schemaJson = EipSchemaTranslationTest.getClassLoader().getResource(path.toString()).text

        Gson gson = TestIOUtils.configureGson()
        def eipComponentListType = new TypeToken<Map<String, List<EipComponent>>>() {}
        return EipSchema.from(gson.fromJson(schemaJson, eipComponentListType))
    }
}
