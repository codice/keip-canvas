package org.codice.keip.xsd.xml.attribute;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnnotationTranslator {

  private static final Pattern whitespacePattern = Pattern.compile("\\s+");

  public String getDescription(XmlSchemaElement element) {
    if (element.getAnnotation() != null) {
      return getDescription(element.getAnnotation());
    }
    if (element.getSchemaType() != null) {
      return getDescription(element.getSchemaType().getAnnotation());
    }
    return null;
  }

  public String getDescription(XmlSchemaAttribute attribute) {
    return getDescription(attribute.getAnnotation());
  }

  // TODO: Handle the appInfo/tool ref annotations
  String getDescription(XmlSchemaAnnotation annotation) {
    if (annotation != null && annotation.getItems() != null) {
      String result =
          annotation.getItems().stream()
              .map(this::getMarkup)
              .filter(Objects::nonNull)
              .map(this::getTextContent)
              .filter(Predicate.not(String::isBlank))
              .collect(Collectors.joining(" "));
      return result.isBlank() ? null : result;
    }
    return null;
  }

  private NodeList getMarkup(XmlSchemaAnnotationItem item) {
    return switch (item) {
      case XmlSchemaAppInfo appInfo -> appInfo.getMarkup();
      case XmlSchemaDocumentation doc -> doc.getMarkup();
      default -> throw new IllegalStateException("Unexpected value: " + item);
    };
  }

  private String getTextContent(NodeList nodeList) {
    var sj = new StringJoiner(" ");
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      String content = node.getTextContent().trim();
      sj.add(whitespacePattern.matcher(content).replaceAll(" "));
    }
    return sj.toString();
  }
}
