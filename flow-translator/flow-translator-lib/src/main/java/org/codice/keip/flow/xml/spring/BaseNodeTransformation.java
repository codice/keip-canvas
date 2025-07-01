package org.codice.keip.flow.xml.spring;

import org.codice.keip.flow.model.EipChild;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.xml.XmlElement;
import java.util.List;

/**
 * Handles simple conversion from an {@link EipNode} to an {@link XmlElement}. No additions or
 * modifications are made to the input node's attributes or children. The helper methods defined
 * here are intended for use by {@link org.codice.keip.flow.xml.NodeTransformer} implementations as
 * the first step in the transformation.
 */
public final class BaseNodeTransformation {

  public static XmlElement toXmlElement(EipNode node) {
    List<XmlElement> children =
        node.children().stream().map(c -> toXmlElement(c, node.eipId().namespace())).toList();
    return new XmlElement(
        node.eipId().namespace(), node.eipId().name(), node.attributes(), children);
  }

  private static XmlElement toXmlElement(EipChild child, String prefix) {
    List<XmlElement> children =
        child.children().stream().map(c -> toXmlElement(c, prefix)).toList();
    return new XmlElement(prefix, child.name(), child.attributes(), children);
  }
}
