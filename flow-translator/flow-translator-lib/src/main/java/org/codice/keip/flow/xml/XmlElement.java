package org.codice.keip.flow.xml;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

public record XmlElement(QName qname, Map<String, Object> attributes, List<XmlElement> children) {

  public XmlElement(
      String prefix, String localName, Map<String, Object> attributes, List<XmlElement> children) {
    this(new QName(null, localName, prefix), attributes, children);
  }

  public String localName() {
    return qname.getLocalPart();
  }

  public String namespaceUri() {
    return qname.getNamespaceURI();
  }

  public String prefix() {
    return qname.getPrefix();
  }
}
