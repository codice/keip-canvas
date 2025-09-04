package org.codice.keip.flow.xml;

import com.ctc.wstx.stax.WstxInputFactory;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.graph.GuavaGraph;
import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.xml.spring.ChannelEdgeExtractor;

public abstract class GraphXmlParser {

  private final XMLInputFactory inputFactory = initializeXMLInputFactory();

  private final Map<String, String> xmlToEipNamespaceMap;

  public GraphXmlParser(Collection<NamespaceSpec> namespaceSpecs) {
    this.xmlToEipNamespaceMap =
        namespaceSpecs.stream()
            .collect(Collectors.toMap(NamespaceSpec::xmlNamespace, NamespaceSpec::eipNamespace));
  }

  protected abstract QName rootElement();

  protected abstract XmlElementTransformer getXmlElementTransformer();

  // TODO: Preserve node descriptions
  // TODO: Consider deprecating the label field on the EipNode (use id only)
  // TODO: Should schema and registry be passed in ctor instead?
  // TODO: handle custom entities
  public final XmlTranslationOutput fromXml(
      Reader xml, XMLValidationSchema schema, ComponentRegistry registry)
      throws TransformerException {
    List<EipNode> nodes = new ArrayList<>();
    List<TransformationError> errors = new ArrayList<>();

    try {
      // XmlEventReader does not support validation while parsing. Using XmlStreamReader instead.
      XMLStreamReader2 streamReader = (XMLStreamReader2) inputFactory.createXMLStreamReader(xml);
      if (schema != null) {
        streamReader.validateAgainst(schema);
      }

      XMLStreamReader reader = inputFactory.createFilteredReader(streamReader, this::elementFilter);
      XmlElementTransformer xmlElementTransformer = getXmlElementTransformer();

      while (reader.hasNext()) {
        // TODO: keep parsing even after an error?
        XmlElement element = parseElement(reader);
        nodes.add(xmlElementTransformer.apply(element, registry));
      }
    } catch (XMLStreamException | RuntimeException e) {
      throw new TransformerException(e);
    }

    // TODO: Normalize graph (replace direct channels with edges)
    GuavaGraph graph = new ChannelEdgeExtractor(nodes).buildGraph();

    return new XmlTranslationOutput(graph, errors);
  }

  private XmlElement parseElement(XMLStreamReader reader)
      throws TransformerException, XMLStreamException {
    Deque<XmlElement> parentStack = new ArrayDeque<>();

    XmlElement top = null;
    while (reader.hasNext()) {
      if (reader.isStartElement()) {
        XmlElement current = createElement(reader);
        if (!parentStack.isEmpty()) {
          parentStack.peek().children().add(current);
        }
        parentStack.push(current);
      } else if (reader.isEndElement()) {
        top = parentStack.pop();
      }

      reader.next();
      if (parentStack.isEmpty()) {
        break;
      }
    }

    return top;
  }

  private XmlElement createElement(XMLStreamReader reader) throws TransformerException {
    String prefix = xmlToEipNamespaceMap.get(reader.getNamespaceURI());
    if (prefix == null) {
      throw new TransformerException(
          String.format("Unregistered namespace: %s", reader.getNamespaceURI()));
    }

    XmlElement element =
        new XmlElement(prefix, reader.getLocalName(), new LinkedHashMap<>(), new ArrayList<>());

    for (int i = 0; i < reader.getAttributeCount(); i++) {
      element.attributes().put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
    }

    return element;
  }

  private boolean elementFilter(XMLStreamReader reader) {
    // skip root element
    if (reader.hasName() && reader.getName().equals(rootElement())) {
      return false;
    }

    return reader.isStartElement() || reader.isEndElement();
  }

  static XMLInputFactory initializeXMLInputFactory() {
    XMLInputFactory factory = WstxInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
