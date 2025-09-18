package org.codice.keip.flow.xml;

import static org.codice.keip.flow.xml.spring.AttributeNames.ID;

import com.ctc.wstx.stax.WstxEventFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Schema;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// TODO: Javadoc
public abstract class GraphXmlParser {

  private final XMLOutputFactory outputFactory = WstxOutputFactory.newFactory();
  private final XmlElementWriter elementWriter =
      new XmlElementWriter(WstxEventFactory.newFactory());
  private final Map<String, String> xmlToEipNamespaceMap;

  private final ComponentRegistry registry;

  private Schema validationSchema;

  public GraphXmlParser(Collection<NamespaceSpec> namespaceSpecs, ComponentRegistry registry) {
    this.xmlToEipNamespaceMap =
        namespaceSpecs.stream()
            .collect(Collectors.toMap(NamespaceSpec::xmlNamespace, NamespaceSpec::eipNamespace));
    this.registry = registry;
  }

  public void setValidationSchema(Schema validationSchema) {
    this.validationSchema = validationSchema;
  }

  protected abstract boolean isCustomEntity(QName name);

  protected abstract XmlElementTransformer getXmlElementTransformer();

  protected abstract GraphEdgeBuilder graphEdgeBuilder();

  public record XmlParseResult(EipGraph graph, Map<String, String> customEntities) {}

  // TODO: Preserve node descriptions
  // TODO: Consider deprecating the label field on the EipNode (use id only)

  // TODO: add note on dom vs stax
  public final XmlParseResult fromXml(InputStream xml) throws TransformerException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    if (validationSchema != null) {
      factory.setSchema(validationSchema);
    }

    final List<EipNode> nodes;
    final Map<String, String> customEntities = new LinkedHashMap<>();

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new ParsingErrorHandler());
      Document doc = builder.parse(xml);
      nodes = parseTopLevelElements(doc.getDocumentElement(), customEntities);
    } catch (SAXException e) {
      throw new TransformerException("Failed to validate input xml", e);
    } catch (ParserConfigurationException | IOException e) {
      throw new TransformerException("Failed to parse input xml", e);
    } catch (RuntimeException | XMLStreamException e) {
      throw new TransformerException(e);
    }

    EipGraph graph = graphEdgeBuilder().toGraph(nodes);
    return new XmlParseResult(graph, customEntities);
  }

  // Walks the through each top-level node, transforms into an EipNode, and adds it to the returned
  // 'nodes' list.
  private List<EipNode> parseTopLevelElements(Element root, Map<String, String> customEntities)
      throws XMLStreamException, TransformerException {
    List<EipNode> nodes = new ArrayList<>();
    Node child = root.getFirstChild();
    while (child != null) {
      handleXmlNode(child, nodes, customEntities);
      child = child.getNextSibling();
    }
    return nodes;
  }

  // Parses the subtree for the provided node into an XmlElement. If the element is a custom
  // entity, save it to the 'customEntities' map, otherwise transform to an EipNode and add to
  // 'nodes' list.
  private void handleXmlNode(Node node, List<EipNode> nodes, Map<String, String> customEntities)
      throws TransformerException, XMLStreamException {
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      XmlElement element = parseElement((Element) node);
      if (isCustomEntity(element.qname())) {
        CustomEntity entity = toCustomEntity(element);
        customEntities.put(entity.id(), entity.xml());
      } else {
        nodes.add(getXmlElementTransformer().apply(element, registry));
      }
    }
  }

  private XmlElement parseElement(Element node) throws TransformerException {
    XmlElement parentElement = createXmlElement(node);
    Node child = node.getFirstChild();
    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        XmlElement childElement = parseElement((Element) child);
        parentElement.children().add(childElement);
      }
      child = child.getNextSibling();
    }
    return parentElement;
  }

  private XmlElement createXmlElement(Element node) throws TransformerException {
    QName name = new QName(node.getNamespaceURI(), node.getLocalName(), getEipPrefix(node));
    XmlElement element = new XmlElement(name, new LinkedHashMap<>(), new ArrayList<>());
    NamedNodeMap attrs = node.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      Attr attr = (Attr) attrs.item(i);
      if (attr.getSpecified()) {
        element.attributes().put(attr.getName(), attr.getValue());
      }
    }
    return element;
  }

  private String getEipPrefix(Element e) throws TransformerException {
    if (isCustomEntity(toQName(e))) {
      return "";
    }

    String prefix = xmlToEipNamespaceMap.get(e.getNamespaceURI());
    if (prefix == null) {
      throw new TransformerException(
          String.format("Unregistered namespace: %s", e.getNamespaceURI()));
    }
    return prefix;
  }

  private QName toQName(Element e) {
    String prefix = e.getPrefix() == null ? "" : e.getPrefix();
    return new QName(e.getNamespaceURI(), e.getLocalName(), prefix);
  }

  private CustomEntity toCustomEntity(XmlElement element)
      throws TransformerException, XMLStreamException {
    String id = removeId(element);

    Writer sw = new StringWriter();
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(sw);
    elementWriter.write(element, eventWriter);
    eventWriter.flush();
    eventWriter.close();

    return new CustomEntity(id, sw.toString());
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

  private record CustomEntity(String id, String xml) {}

  private static class ParsingErrorHandler implements ErrorHandler {
    @Override
    public void warning(SAXParseException exception) {}

    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }
  }
}
