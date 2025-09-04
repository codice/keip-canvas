package org.codice.keip.flow.xml.spring;

import static org.codice.keip.flow.xml.spring.AttributeNames.ID;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.model.EipChild;
import org.codice.keip.flow.model.EipId;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.xml.XmlElement;
import org.codice.keip.flow.xml.XmlElementTransformer;

/** A default implementation for generating an {@link EipNode} from an {@link XmlElement} */
public class DefaultXmlElementTransformer implements XmlElementTransformer {

  @Override
  public EipNode apply(XmlElement element, ComponentRegistry registry) throws TransformerException {
    validateElement(element, registry);

    EipId eipId = new EipId(element.prefix(), element.localName());
    Map<String, Object> filteredAttrs = new LinkedHashMap<>(element.attributes());
    filteredAttrs.remove(ID);

    List<EipChild> children = element.children().stream().map(this::convertChild).toList();

    return new EipNode(
        element.attributes().get(ID).toString(),
        eipId,
        null,
        null,
        registry.getRole(eipId),
        registry.getConnectionType(eipId),
        filteredAttrs,
        children);
  }

  private EipChild convertChild(XmlElement element) {
    List<EipChild> children = element.children().stream().map(this::convertChild).toList();
    return new EipChild(element.localName(), element.attributes(), children);
  }

  private void validateElement(XmlElement element, ComponentRegistry registry)
      throws TransformerException {
    if (!element.attributes().containsKey(ID)) {
      throw new TransformerException(
          String.format(
              "%s:%s element does not have an 'id' attribute",
              element.prefix(), element.localName()));
    }

    EipId id = new EipId(element.prefix(), element.localName());
    if (!registry.isRegistered(id)) {
      throw new TransformerException(
          String.format(
              "%s:%s is not a supported EIP Component", element.prefix(), element.localName()));
    }
  }
}
