package org.codice.keip.flow.xml;

import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;

final class XmlElementWriter {

  private final XMLEventFactory eventFactory;

  public XmlElementWriter(XMLEventFactory eventFactory) {
    this.eventFactory = eventFactory;
  }

  void write(XmlElement element, XMLEventWriter writer) {
    try {
      writer.add(
          this.eventFactory.createStartElement(
              element.prefix(),
              element.namespaceUri(),
              element.localName(),
              attributeIterator(element.attributes()),
              null));

      for (XmlElement c : element.children()) {
        write(c, writer);
      }

      writer.add(
          this.eventFactory.createEndElement(
              element.prefix(), element.namespaceUri(), element.localName()));
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  private Iterator<Attribute> attributeIterator(Map<String, Object> attributes) {
    return attributes.entrySet().stream()
        .map(e -> this.eventFactory.createAttribute(e.getKey(), e.getValue().toString()))
        .iterator();
  }
}
