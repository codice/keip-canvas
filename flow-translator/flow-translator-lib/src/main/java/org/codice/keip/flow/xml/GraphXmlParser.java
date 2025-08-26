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

/**
 * Parses an XML document into an intermediate {@link EipGraph} representation. This base class
 * handles the general transformation process, concrete subclasses are responsible for implementing
 * platform-specific parsing logic.
 */
public abstract class GraphXmlParser {

  // TODO: Preserve node descriptions
  // TODO: Consider deprecating the label field on the EipNode (use id only)

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

  /**
   * Parses an XML into an {@link EipGraph} instance.
   *
   * @param xml input xml
   * @return an {@link XmlParseResult} containing the parsed graph and any custom entities
   * @throws TransformerException thrown if a critical error is encountered while parsing
   *     <h4>DOM vs StAX API for parsing</h4>
   *     <p>We considered using the Stax API (streaming) to parse the input XML without loading the
   *     entire document into memory. However, unlike the DOM API, StAX (including the StAX2
   *     extension) does not support the standard {@link Schema} interface. Instead, it relies on a
   *     separate {@link org.codehaus.stax2.validation.XMLValidationSchema}, which offers more
   *     limited functionality when working with multiple schemas.
   *     <p>To avoid introducing a complex build step where multiple schemas would need to be
   *     manually dereferenced and combined, we use the DOM API for validation. Since DOM-based
   *     validation already requires reading the full XML document into memory, it is practical to
   *     use DOM for parsing as well, as the benefits of streaming are largely negated at that
   *     point.
   *     <p>While the DOM model is generally more memory-intensive and may incur a performance cost,
   *     we expect the provided EIP XML documents to be relatively small (on the order of
   *     kilobytes). Therefore, the impact is likely negligible. If future monitoring or
   *     benchmarking indicates that DOM parsing is a bottleneck, a StAX-based approach could be
   *     revisited, either by introducing an XSD preprocessing step (as mentioned above) or by
   *     disabling validation entirely.
   */
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
      throw new IllegalArgumentException("Failed to validate input xml", e);
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

  private XmlElement parseElement(Element node) {
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

  private XmlElement createXmlElement(Element node) {
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

  private String getEipPrefix(Element e) {
    if (isCustomEntity(toQName(e))) {
      return "";
    }

    String prefix = xmlToEipNamespaceMap.get(e.getNamespaceURI());
    if (prefix == null) {
      throw new IllegalArgumentException(
          String.format("Unregistered namespace: %s", e.getNamespaceURI()));
    }
    return prefix;
  }

  private QName toQName(Element e) {
    String prefix = e.getPrefix() == null ? "" : e.getPrefix();
    return new QName(e.getNamespaceURI(), e.getLocalName(), prefix);
  }

  private CustomEntity toCustomEntity(XmlElement element) throws XMLStreamException {
    String id = removeId(element);

    Writer sw = new StringWriter();
    XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(sw);
    elementWriter.write(element, eventWriter);
    eventWriter.flush();
    eventWriter.close();

    return new CustomEntity(id, sw.toString());
  }

  private String removeId(XmlElement element) {
    if (!element.attributes().containsKey(ID)) {
      throw new IllegalArgumentException(
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
