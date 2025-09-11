package org.codice.keip.flow.xml;

import static org.codice.keip.flow.xml.spring.AttributeNames.ID;

import com.ctc.wstx.stax.WstxEventFactory;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;

// TODO: Javadoc
public abstract class GraphXmlParser {

  private final XMLInputFactory inputFactory = initializeXMLInputFactory();
  private final XMLOutputFactory outputFactory = WstxOutputFactory.newFactory();
  private final XmlElementWriter elementWriter =
      new XmlElementWriter(WstxEventFactory.newFactory());
  private final Map<String, String> customEntities = new LinkedHashMap<>();
  private final Map<String, String> xmlToEipNamespaceMap;

  private final ComponentRegistry registry;

  private XMLValidationSchema validationSchema;

  public GraphXmlParser(Collection<NamespaceSpec> namespaceSpecs, ComponentRegistry registry) {
    this.xmlToEipNamespaceMap =
        namespaceSpecs.stream()
            .collect(Collectors.toMap(NamespaceSpec::xmlNamespace, NamespaceSpec::eipNamespace));
    this.registry = registry;
  }

  public void setValidationSchema(XMLValidationSchema validationSchema) {
    this.validationSchema = validationSchema;
  }

  protected abstract QName rootElement();

  protected abstract boolean isCustomEntity(QName name);

  protected abstract XmlElementTransformer getXmlElementTransformer();

  protected abstract GraphEdgeBuilder graphEdgeBuilder();

  public record XmlParseResult(
      EipGraph graph, Map<String, String> customEntities, List<TransformationError> errors) {}

  // TODO: Preserve node descriptions
  // TODO: Consider deprecating the label field on the EipNode (use id only)
  public final XmlParseResult fromXml(Reader xml) throws TransformerException {
    List<EipNode> nodes = new ArrayList<>();
    List<TransformationError> errors = new ArrayList<>();

    try {
      // XmlEventReader does not support validation while parsing. Using XmlStreamReader instead.
      XMLStreamReader2 streamReader = (XMLStreamReader2) inputFactory.createXMLStreamReader(xml);
      if (validationSchema != null) {
        streamReader.validateAgainst(validationSchema);
      }

      XMLStreamReader reader = inputFactory.createFilteredReader(streamReader, this::elementFilter);
      XmlElementTransformer xmlElementTransformer = getXmlElementTransformer();

      while (reader.hasNext()) {
        // TODO: keep parsing even after an error?
        XmlElement element = parseElement(reader);
        if (isCustomEntity(element.qname())) {
          addCustomEntity(element);
        } else {
          nodes.add(xmlElementTransformer.apply(element, registry));
        }
      }
    } catch (XMLStreamException | RuntimeException e) {
      throw new TransformerException(e);
    }

    EipGraph graph = graphEdgeBuilder().toGraph(nodes);
    return new XmlParseResult(graph, customEntities, errors);
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
    String prefix = getPrefix(reader);
    XmlElement element =
        new XmlElement(
            new QName(reader.getNamespaceURI(), reader.getLocalName(), prefix),
            new LinkedHashMap<>(),
            new ArrayList<>());

    for (int i = 0; i < reader.getAttributeCount(); i++) {
      element.attributes().put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
    }

    return element;
  }

  private String getPrefix(XMLStreamReader reader) throws TransformerException {
    if (isCustomEntity(reader.getName())) {
      return "";
    }

    String prefix = xmlToEipNamespaceMap.get(reader.getNamespaceURI());
    if (prefix == null) {
      throw new TransformerException(
          String.format("Unregistered namespace: %s", reader.getNamespaceURI()));
    }
    return prefix;
  }

  private void addCustomEntity(XmlElement element) throws TransformerException, XMLStreamException {
    String id = removeId(element);

    Writer sw = new StringWriter();
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(sw);
    elementWriter.write(element, eventWriter);
    eventWriter.flush();
    eventWriter.close();

    customEntities.put(id, sw.toString());
  }

  private String removeId(XmlElement element) throws TransformerException {
    if (!element.attributes().containsKey(ID)) {
      throw new TransformerException(
          String.format("%s element does not have an 'id' attribute", element.localName()));
    }
    String id = element.attributes().get(ID).toString();
    element.attributes().remove(ID);
    return id;
  }

  private boolean elementFilter(XMLStreamReader reader) {
    // skip root element
    if (reader.hasName() && reader.getName().equals(rootElement())) {
      return false;
    }

    return reader.isStartElement() || reader.isEndElement();
  }

  private static XMLInputFactory initializeXMLInputFactory() {
    XMLInputFactory factory = WstxInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
