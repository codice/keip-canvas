package org.codice.keip.schema.xml.attribute;

import org.codice.keip.schema.model.eip.Attribute;
import org.codice.keip.schema.model.eip.AttributeType;
import org.codice.keip.schema.model.eip.Restriction;
import java.util.Collections;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaBaseSimpleType;
import org.apache.ws.commons.schema.walker.XmlSchemaRestriction;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;

public class AttributeTranslator {

  private final AnnotationTranslator annotationTranslator;

  public AttributeTranslator() {
    this.annotationTranslator = new AnnotationTranslator();
  }

  public AttributeTranslator(AnnotationTranslator annotationTranslator) {
    this.annotationTranslator = annotationTranslator;
  }

  public Attribute translate(XmlSchemaAttrInfo attrInfo) {
    validateAttributeInfo(attrInfo);
    XmlSchemaAttribute attribute = attrInfo.getAttribute();
    XmlSchemaTypeInfo typeInfo = attrInfo.getType();

    Attribute.Builder builder =
        new Attribute.Builder(attribute.getName(), getType(typeInfo))
            .description(annotationTranslator.getDescription(attribute))
            .defaultValue(attribute.getDefaultValue());

    if (XmlSchemaUse.REQUIRED.equals(attribute.getUse())) {
      builder.required(true);
    }

    Restriction restriction = getRestriction(typeInfo);
    if (restriction != null) {
      builder.restriction(restriction);
    }

    return builder.build();
  }

  private AttributeType getType(XmlSchemaTypeInfo typeInfo) {
    if (typeInfo == null) {
      return toAttributeType(XmlSchemaBaseSimpleType.STRING);
    }

    return switch (typeInfo.getType()) {
      case LIST -> throw new IllegalStateException("List types are unsupported");
      case UNION -> resolveUnionType(typeInfo);
      case ATOMIC -> toAttributeType(typeInfo.getBaseType());
      case COMPLEX ->
          throw new IllegalStateException(
              "TODO: Is it even possible to have complex-typed attributes?");
    };
  }

  private AttributeType toAttributeType(XmlSchemaBaseSimpleType simpleType) {
    return switch (simpleType) {
      case BOOLEAN -> AttributeType.BOOLEAN;
      case DECIMAL, DOUBLE, FLOAT -> AttributeType.NUMBER;
      default -> AttributeType.STRING;
    };
  }

  private AttributeType resolveUnionType(XmlSchemaTypeInfo typeInfo) {
    for (XmlSchemaTypeInfo childType : typeInfo.getChildTypes()) {
      // Take the first type
      if (childType.getBaseType() != null) {
        return toAttributeType(childType.getBaseType());
      }
    }
    return AttributeType.STRING;
  }

  private Restriction getRestriction(XmlSchemaTypeInfo typeInfo) {
    if (typeInfo == null) {
      return null;
    }

    List<String> enumRestrictions = getEnumRestrictions(typeInfo);
    if (!enumRestrictions.isEmpty()) {
      return new Restriction.MultiValuedRestriction(
          Restriction.RestrictionType.ENUM, enumRestrictions);
    }

    if (typeInfo.getChildTypes() != null) {
      // Take the first restriction
      for (XmlSchemaTypeInfo childType : typeInfo.getChildTypes()) {
        Restriction restriction = getRestriction(childType);
        if (restriction != null) {
          return restriction;
        }
      }
    }

    return null;
  }

  private List<String> getEnumRestrictions(XmlSchemaTypeInfo typeInfo) {
    if (typeInfo.getFacets() == null) {
      return Collections.emptyList();
    }

    List<XmlSchemaRestriction> restrictions =
        typeInfo.getFacets().get(XmlSchemaRestriction.Type.ENUMERATION);

    if (restrictions == null) {
      return Collections.emptyList();
    }

    return restrictions.stream().map(restriction -> restriction.getValue().toString()).toList();
  }

  private void validateAttributeInfo(XmlSchemaAttrInfo attrInfo) {
    XmlSchemaAttribute attribute = attrInfo.getAttribute();
    XmlSchemaTypeInfo typeInfo = attrInfo.getType();

    if (XmlSchemaUse.PROHIBITED.equals(attribute.getUse())) {
      throw new IllegalArgumentException(
          "Prohibited use attributes are not supported: " + attribute.getName());
    }

    if (typeInfo != null && typeInfo.isMixed()) {
      throw new IllegalArgumentException(
          "Mixed complex type elements are not supported: " + attribute.getName());
    }
  }
}
