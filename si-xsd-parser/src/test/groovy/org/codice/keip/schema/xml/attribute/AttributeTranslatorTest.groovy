package org.codice.keip.schema.xml.attribute

import org.codice.keip.schema.model.eip.AttributeType
import org.codice.keip.schema.model.eip.Restriction
import org.apache.ws.commons.schema.XmlSchema
import org.apache.ws.commons.schema.XmlSchemaAttribute
import org.apache.ws.commons.schema.XmlSchemaUse
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType
import org.apache.ws.commons.schema.walker.XmlSchemaRestriction
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo
import spock.lang.Specification

class AttributeTranslatorTest extends Specification {

    static final DESCRIPTION = "test-description"

    static final ATTRIBUTE_NAME = "test-attr"

    AttributeTranslator attributeTranslator = new AttributeTranslator(mockAnnotationTranslator(DESCRIPTION))

    XmlSchemaAttribute xmlAttribute

    void setup() {
        xmlAttribute = new XmlSchemaAttribute(new XmlSchema("test-ns", null), false)
        xmlAttribute.setName(ATTRIBUTE_NAME)
    }

    def "Translate mixed type attribute is not supported"() {
        given:
        def typeInfo = new XmlSchemaTypeInfo(true)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        attributeTranslator.translate(attributeInfo)

        then:
        thrown(IllegalArgumentException)
    }

    def "Translate prohibited use attribute is not supported"() {
        given:
        xmlAttribute.setUse(XmlSchemaUse.PROHIBITED)
        def typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        attributeTranslator.translate(attributeInfo)

        then:
        thrown(IllegalArgumentException)
    }

    def "Translate an 'atomic' type string attribute that is fully specified"() {
        given:
        xmlAttribute.setDefaultValue("test-default")
        xmlAttribute.setUse(XmlSchemaUse.REQUIRED)

        def typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.name() == ATTRIBUTE_NAME
        attribute.type() == AttributeType.STRING
        attribute.description() == DESCRIPTION
        attribute.defaultValue() == "test-default"
        attribute.required()
        attribute.restriction() == null
    }

    def "Translate an 'atomic' type string attribute that is minimally specified"() {
        given:
        def typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)
        attributeTranslator = new AttributeTranslator(Mock(AnnotationTranslator))

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.name() == ATTRIBUTE_NAME
        attribute.type() == AttributeType.STRING
        attribute.description() == null
        attribute.defaultValue() == null
        !attribute.required()
        attribute.restriction() == null
    }

    def "Translate different attribute use types, only the 'REQUIRED' use type sets the attribute.required boolean"(XmlSchemaUse useType, boolean isRequired) {
        given:
        xmlAttribute.setUse(useType)
        def typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.required() == isRequired

        where:
        useType               | isRequired
        XmlSchemaUse.NONE     | false
        XmlSchemaUse.OPTIONAL | false
        XmlSchemaUse.REQUIRED | true
    }

    def "Translate null type results in a string type attribute with no restrictions"() {
        given:
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, null)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.name() == ATTRIBUTE_NAME
        attribute.type() == AttributeType.STRING
        attribute.restriction() == null
    }

    def "Translate different 'atomic' types to their respective attribute types"(XmlSchemaBaseSimpleType simpleType, String defaultValue, AttributeType resultType) {
        given:
        xmlAttribute.setDefaultValue(defaultValue)
        def typeInfo = new XmlSchemaTypeInfo(simpleType)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.name() == ATTRIBUTE_NAME
        attribute.type() == resultType
        attribute.defaultValue() == defaultValue

        where:
        simpleType                       | defaultValue   | resultType
        XmlSchemaBaseSimpleType.BOOLEAN  | true           | AttributeType.BOOLEAN
        XmlSchemaBaseSimpleType.DECIMAL  | 12.34          | AttributeType.NUMBER
        XmlSchemaBaseSimpleType.DECIMAL  | 12             | AttributeType.NUMBER
        XmlSchemaBaseSimpleType.DECIMAL  | -12            | AttributeType.NUMBER
        XmlSchemaBaseSimpleType.DOUBLE   | 12.34          | AttributeType.NUMBER
        XmlSchemaBaseSimpleType.FLOAT    | -12.34         | AttributeType.NUMBER
        XmlSchemaBaseSimpleType.STRING   | "test-default" | AttributeType.STRING
        XmlSchemaBaseSimpleType.DURATION | "5m"           | AttributeType.STRING
        XmlSchemaBaseSimpleType.YEAR     | "2000"         | AttributeType.STRING
    }

    def "Translate 'list' type is not supported"() {
        given:
        def typeInfoListElement = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)
        def typeInfo = new XmlSchemaTypeInfo(typeInfoListElement)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        attributeTranslator.translate(attributeInfo)

        then:
        thrown(IllegalStateException)
    }

    def "Translate 'complex' type is not supported"() {
        given:
        def typeInfo = new XmlSchemaTypeInfo(false)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        attributeTranslator.translate(attributeInfo)

        then:
        thrown(IllegalStateException)
    }

    def "Translate 'atomic' type with enum restriction"(XmlSchemaBaseSimpleType enumType, List enumValues, AttributeType resultType) {
        given:
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> enumFacets = [(XmlSchemaRestriction.Type.ENUMERATION): createEnumRestrictions(enumValues)]
        def typeInfo = new XmlSchemaTypeInfo(enumType, enumFacets)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.type() == resultType
        attribute.restriction() == new Restriction.MultiValuedRestriction(Restriction.RestrictionType.ENUM, enumValues.collect { it::toString() })

        where:
        enumType                        | enumValues                   | resultType
        XmlSchemaBaseSimpleType.STRING  | ["first", "second", "third"] | AttributeType.STRING
        XmlSchemaBaseSimpleType.STRING  | ["first"]                    | AttributeType.STRING
        XmlSchemaBaseSimpleType.DECIMAL | [1, 2]                       | AttributeType.NUMBER
    }

    def "Translate 'atomic' type with enum restriction ignoring other restriction types"() {
        given:
        def enumValues = ["first", "second"]
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> enumFacets = [(XmlSchemaRestriction.Type.ENUMERATION): createEnumRestrictions(enumValues), (XmlSchemaRestriction.Type.LENGTH): [new XmlSchemaRestriction(XmlSchemaRestriction.Type.LENGTH, 10, false)]]
        def typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, enumFacets)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.type() == AttributeType.STRING
        attribute.restriction() == new Restriction.MultiValuedRestriction(Restriction.RestrictionType.ENUM, enumValues)
    }

    def "Translate 'atomic' type ignoring non enum restriction types"() {
        given:
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> enumFacets = [(XmlSchemaRestriction.Type.LENGTH): [new XmlSchemaRestriction(XmlSchemaRestriction.Type.LENGTH, 10, false)]]
        def typeInfo = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, enumFacets)
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.type() == AttributeType.STRING
        attribute.restriction() == null
    }

    def "Translate 'union' type with enum restriction as only memberType"(XmlSchemaBaseSimpleType enumType, List enumValues, AttributeType resultType) {
        given:
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> enumFacets = [(XmlSchemaRestriction.Type.ENUMERATION): createEnumRestrictions(enumValues)]
        def typeInfoUnionElement = new XmlSchemaTypeInfo(enumType, enumFacets)
        def typeInfo = new XmlSchemaTypeInfo([typeInfoUnionElement])
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.type() == resultType
        attribute.restriction() == new Restriction.MultiValuedRestriction(Restriction.RestrictionType.ENUM, enumValues.collect { it::toString() })

        where:
        enumType                        | enumValues                   | resultType
        XmlSchemaBaseSimpleType.STRING  | ["first", "second", "third"] | AttributeType.STRING
        XmlSchemaBaseSimpleType.STRING  | ["first"]                    | AttributeType.STRING
        XmlSchemaBaseSimpleType.DECIMAL | [1, 2]                       | AttributeType.NUMBER
    }

    def "Translate 'union' type with several memberTypes choose first type"() {
        given:
        def typeInfo = new XmlSchemaTypeInfo([new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.DECIMAL), new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)])
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.type() == AttributeType.NUMBER
        attribute.restriction() == null
    }

    def "Translate 'union' type with several enum restriction memberTypes choose first type"() {
        given:
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> enumFacets1 = [(XmlSchemaRestriction.Type.ENUMERATION): createEnumRestrictions(["first", "second"])]
        def enumRestrictionType1 = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, enumFacets1)
        HashMap<XmlSchemaRestriction.Type, List<XmlSchemaRestriction>> enumFacets2 = [(XmlSchemaRestriction.Type.ENUMERATION): createEnumRestrictions(["second", "third"])]
        def enumRestrictionType2 = new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING, enumFacets2)
        def typeInfo = new XmlSchemaTypeInfo([enumRestrictionType1, enumRestrictionType2, new XmlSchemaTypeInfo(XmlSchemaBaseSimpleType.STRING)])
        def attributeInfo = new XmlSchemaAttrInfo(xmlAttribute, typeInfo)

        when:
        def attribute = attributeTranslator.translate(attributeInfo)

        then:
        attribute.type() == AttributeType.STRING
        attribute.restriction() == new Restriction.MultiValuedRestriction(Restriction.RestrictionType.ENUM, ["first", "second"])
    }

    private static List<XmlSchemaRestriction> createEnumRestrictions(List enums) {
        return enums.collect { new XmlSchemaRestriction(XmlSchemaRestriction.Type.ENUMERATION, it, false) }
    }

    private AnnotationTranslator mockAnnotationTranslator(String description) {
        def mockTranslator = Mock(AnnotationTranslator)
        mockTranslator.getDescription(_) >> description
        return mockTranslator
    }
}
