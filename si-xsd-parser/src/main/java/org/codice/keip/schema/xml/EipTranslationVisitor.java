package org.codice.keip.schema.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupParticle;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.walker.XmlSchemaAttrInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo;
import org.apache.ws.commons.schema.walker.XmlSchemaTypeInfo.Type;
import org.apache.ws.commons.schema.walker.XmlSchemaVisitor;
import org.codice.keip.schema.model.eip.Attribute;
import org.codice.keip.schema.model.eip.ChildComposite;
import org.codice.keip.schema.model.eip.ChildGroup;
import org.codice.keip.schema.model.eip.ConnectionType;
import org.codice.keip.schema.model.eip.EipChildElement;
import org.codice.keip.schema.model.eip.EipComponent;
import org.codice.keip.schema.model.eip.Indicator;
import org.codice.keip.schema.model.eip.Occurrence;
import org.codice.keip.schema.model.eip.Role;
import org.codice.keip.schema.xml.attribute.AnnotationTranslator;
import org.codice.keip.schema.xml.attribute.AttributeTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom {@link XmlSchemaVisitor} that parses an Integration XSD into the {@link
 * org.codice.keip.schema.model.eip.EipSchema} model.
 *
 * <p>TODO: Consider switching to a reference-based schema (rather than inlining children). Could
 * potentially decrease the size of output files and enable us to better handle circular refs.
 */
public class EipTranslationVisitor implements XmlSchemaVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(EipTranslationVisitor.class);

  private final AttributeTranslator attributeTranslator;

  private final AnnotationTranslator annotationTranslator;

  private final Map<QName, EipChildElement> visitedElements;

  private final Map<QName, EipChildElement> visitedBaseTypes;

  private EipComponent.Builder eipComponentBuilder;

  private ChildCompositeWrapper currElement;

  public EipTranslationVisitor() {
    this.attributeTranslator = new AttributeTranslator();
    this.annotationTranslator = new AnnotationTranslator();
    this.visitedElements = new HashMap<>();
    this.visitedBaseTypes = new HashMap<>();
  }

  public EipComponent getEipComponent() {
    return eipComponentBuilder.build();
  }

  public void reset() {
    eipComponentBuilder = null;
    currElement = null;
  }

  @Override
  public void onEnterElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean visited) {
    if (xmlSchemaElement.isTopLevel()) {
      eipComponentBuilder = initTopLevelEipComponent(xmlSchemaElement);
      return;
    }

    EipChildElement element;
    if (visited) {
      element = retrieveVisitedElement(xmlSchemaElement);
    } else {
      element =
          new EipChildElement.Builder(xmlSchemaElement.getName())
              .occurrence(getOccurrence(xmlSchemaElement))
              .description(annotationTranslator.getDescription(xmlSchemaElement))
              .build();
      storeVisitedElement(xmlSchemaElement, xmlSchemaTypeInfo, element);
    }

    var wrapper = new ChildCompositeWrapper(element, currElement);
    currElement.wrappedChild.addChild(element);
    currElement = wrapper;
  }

  @Override
  public void onExitElement(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo, boolean visited) {
    if (xmlSchemaElement.isTopLevel()) {
      eipComponentBuilder.connectionType(
          getConnectionType(xmlSchemaElement, eipComponentBuilder.build()));
      return;
    }
    exitNode();
  }

  @Override
  public void onVisitAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAttrInfo xmlSchemaAttrInfo) {
    Attribute attribute = attributeTranslator.translate(xmlSchemaAttrInfo);
    if (xmlSchemaElement.isTopLevel()) {
      eipComponentBuilder.addAttribute(attribute);
    } else if (currElement.wrappedChild instanceof EipChildElement child) {
      child.addAttribute(attribute);
    }
  }

  @Override
  public void onEndAttributes(
      XmlSchemaElement xmlSchemaElement, XmlSchemaTypeInfo xmlSchemaTypeInfo) {}

  @Override
  public void onEnterSubstitutionGroup(XmlSchemaElement xmlSchemaElement) {}

  @Override
  public void onExitSubstitutionGroup(XmlSchemaElement xmlSchemaElement) {}

  @Override
  public void onEnterAllGroup(XmlSchemaAll xmlSchemaAll) {
    enterGroup(Indicator.ALL, xmlSchemaAll);
  }

  @Override
  public void onExitAllGroup(XmlSchemaAll xmlSchemaAll) {
    exitNode();
  }

  @Override
  public void onEnterChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    enterGroup(Indicator.CHOICE, xmlSchemaChoice);
  }

  @Override
  public void onExitChoiceGroup(XmlSchemaChoice xmlSchemaChoice) {
    exitNode();
  }

  @Override
  public void onEnterSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    enterGroup(Indicator.SEQUENCE, xmlSchemaSequence);
  }

  @Override
  public void onExitSequenceGroup(XmlSchemaSequence xmlSchemaSequence) {
    exitNode();
  }

  @Override
  public void onVisitAny(XmlSchemaAny xmlSchemaAny) {
    // TODO: Might need to parse this for arbitrary beans.
  }

  @Override
  public void onVisitAnyAttribute(
      XmlSchemaElement xmlSchemaElement, XmlSchemaAnyAttribute xmlSchemaAnyAttribute) {}

  private void enterGroup(Indicator indicator, XmlSchemaGroupParticle particle) {
    var group = new ChildGroup(indicator, getOccurrence(particle));
    var wrapper = new ChildCompositeWrapper(group, currElement);

    if (currElement == null) {
      // top level child group
      eipComponentBuilder.childGroup(group);
    } else {
      currElement.wrappedChild.addChild(group);
    }

    currElement = wrapper;
  }

  private void exitNode() {
    currElement = currElement.parent();
  }

  private EipComponent.Builder initTopLevelEipComponent(XmlSchemaElement xmlSchemaElement) {
    if (eipComponentBuilder != null) {
      throw new IllegalStateException(
          "The top level element should only be entered once. Was the visitor reset?");
    }
    return new EipComponent.Builder(
            xmlSchemaElement.getName(), getRole(xmlSchemaElement), ConnectionType.PASSTHRU)
        .description(annotationTranslator.getDescription(xmlSchemaElement));
  }

  private EipChildElement retrieveVisitedElement(XmlSchemaElement xmlSchemaElement) {
    if (xmlSchemaElement.getSchemaTypeName() == null) {
      // Check if element is a direct ref
      EipChildElement element = visitedElements.get(xmlSchemaElement.getQName());

      // TODO:
      // For back references (cycles) to be handled properly, direct references need to use the same
      // element (no copying). However, it seems that some references actually have differing
      // descriptions and occurrence.
      // For now, comparing those two fields to determine if we should copy or not, but look for a
      // better solution.
      String refDescription = annotationTranslator.getDescription(xmlSchemaElement);
      if (Objects.equals(element.getDescription(), refDescription)
          && element.occurrence().equals(getOccurrence(xmlSchemaElement))) {
        return element;
      }
      return new EipChildElement.Builder(element)
          .description(refDescription)
          .occurrence(getOccurrence(xmlSchemaElement))
          .build();
    } else {
      // check if element is a type ref
      EipChildElement element = visitedBaseTypes.get(xmlSchemaElement.getSchemaTypeName());

      // Even if SchemaType is the same as a visited element, name/occurrence/description could be
      // different. Create a new child element and copy over the similar properties.
      return new EipChildElement.Builder(element)
          .name(xmlSchemaElement.getName())
          .description(annotationTranslator.getDescription(xmlSchemaElement))
          .occurrence(getOccurrence(xmlSchemaElement))
          .build();
    }
  }

  /**
   * The schema walker only visits an element's attributes once, even if the element is referenced
   * multiple times by the schema. Therefore, newly visited elements and base types need to be
   * cached.
   */
  private void storeVisitedElement(
      XmlSchemaElement xmlSchemaElement,
      XmlSchemaTypeInfo xmlSchemaTypeInfo,
      EipChildElement element) {
    visitedElements.putIfAbsent(xmlSchemaElement.getQName(), element);
    if (xmlSchemaElement.getSchemaTypeName() != null
        && Type.COMPLEX.equals(xmlSchemaTypeInfo.getType())) {
      visitedBaseTypes.putIfAbsent(xmlSchemaElement.getSchemaTypeName(), element);
    }
  }

  // TODO: Refine how roles are determined
  private Role getRole(XmlSchemaElement element) {
    String elementName = element.getName().toLowerCase();
    if (getExtensionBaseName(element).equals("channelType")) {
      return Role.CHANNEL;
    } else if (elementName.contains("router") || elementName.contains("filter")) {
      return Role.ROUTER;
    } else if (elementName.contains("transformer")) {
      return Role.TRANSFORMER;
    }
    return Role.ENDPOINT;
  }

  // TODO: Refine how connection types are determined
  private ConnectionType getConnectionType(XmlSchemaElement element, EipComponent eipComponent) {
    String elementName = element.getName().toLowerCase();
    String extensionBaseName = getExtensionBaseName(element).toLowerCase();

    Set<String> attrNames =
        eipComponent.getAttributes().stream().map(Attribute::name).collect(Collectors.toSet());

    if (elementName.contains("inbound-gateway")) {
      return ConnectionType.INBOUND_REQUEST_REPLY;
    } else if (attrNames.contains("request-channel") && attrNames.contains("reply-channel")) {
      return ConnectionType.REQUEST_REPLY;
    } else if (attrNames.contains("input-channel")
        && attrNames.contains("output-channel")
        && attrNames.contains("discard-channel")) {
      return ConnectionType.TEE;
    } else if (elementName.contains("gateway")) {
      return ConnectionType.REQUEST_REPLY;
    } else if (elementName.contains("filter")) {
      return ConnectionType.TEE;
    } else if (elementName.contains("router")) {
      return ConnectionType.CONTENT_BASED_ROUTER;
    } else if (elementName.contains("inbound")
        || elementName.contains("message-driven")
        || extensionBaseName.contains("inbound")) {
      return ConnectionType.SOURCE;
    } else if (elementName.contains("outbound") || extensionBaseName.contains("outbound")) {
      return ConnectionType.SINK;
    } else {
      return ConnectionType.PASSTHRU;
    }
  }

  private String getExtensionBaseName(XmlSchemaElement element) {
    try {
      if (element.getSchemaType() instanceof XmlSchemaComplexType complexType) {
        XmlSchemaContent content = complexType.getContentModel().getContent();
        if (content instanceof XmlSchemaComplexContentExtension extension) {
          return extension.getBaseTypeName().getLocalPart();
        }
      }
    } catch (NullPointerException e) {
      LOGGER.trace("No base type content defined for: {}", element.getName());
    }
    return "";
  }

  private Occurrence getOccurrence(XmlSchemaParticle particle) {
    return new Occurrence(particle.getMinOccurs(), particle.getMaxOccurs());
  }

  private record ChildCompositeWrapper(ChildComposite wrappedChild, ChildCompositeWrapper parent) {}
}
