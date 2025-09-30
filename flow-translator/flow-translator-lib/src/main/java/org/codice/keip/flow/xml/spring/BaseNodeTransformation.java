package org.codice.keip.flow.xml.spring;

import java.util.List;
import org.codice.keip.flow.model.EipChild;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.xml.XmlElement;

/**
 * Handles simple conversion from an {@link EipNode} to an {@link XmlElement}. No additions or
 * modifications are made to the input node's attributes or children. The helper methods defined
 * here are intended for use by {@link org.codice.keip.flow.xml.NodeTransformer} implementations as
 * the first step in the transformation.
 */
public final class BaseNodeTransformation {

  public static XmlElement toXmlElement(EipNode node) {
    List<XmlElement> children =
        node.children().stream().map(BaseNodeTransformation::toXmlElement).toList();
    return new XmlElement(
        node.eipId().namespace(), node.eipId().name(), node.attributes(), children);
  }

  private static XmlElement toXmlElement(EipChild child) {
    List<XmlElement> children =
        child.children().stream().map(BaseNodeTransformation::toXmlElement).toList();
    return new XmlElement(
        child.eipId().namespace(), child.eipId().name(), child.attributes(), children);
  }
}
