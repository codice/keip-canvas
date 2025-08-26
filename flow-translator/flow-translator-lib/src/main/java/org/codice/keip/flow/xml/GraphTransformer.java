package org.codice.keip.flow.xml;

import static javax.xml.XMLConstants.XML_NS_PREFIX;

import com.ctc.wstx.stax.WstxEventFactory;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.TransformerException;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipId;
import org.codice.keip.flow.model.EipNode;

/**
 * Transforms an intermediate {@link EipGraph} representation to an XML document. This base class
 * takes care of the general transformation process, to create XML targeting specific platforms,
 * extend this class and register specialized {@link NodeTransformer}s.
 */
public abstract class GraphTransformer {

  private static final String XSI_PREFIX = "xsi";

  private final XMLEventFactory eventFactory = WstxEventFactory.newFactory();
  private final XMLOutputFactory outputFactory = WstxOutputFactory.newFactory();
  private final XMLInputFactory inputFactory = initializeXMLInputFactory();
  private final Set<String> reservedPrefixes = collectReservedPrefixes();

  private final NodeTransformerFactory nodeTransformerFactory;
  private final CustomEntityTransformer customEntityTransformer;
  // maps an eipNamespace to a NamespaceSpec
  private final Map<String, NamespaceSpec> registeredNamespaces;

  private final Map<String, String> xmlToEipNamespaceMap;

  protected GraphTransformer(
      NodeTransformerFactory nodeTransformerFactory, Collection<NamespaceSpec> namespaceSpecs) {
    validatePrefixes(namespaceSpecs);
    this.nodeTransformerFactory = nodeTransformerFactory;
    this.customEntityTransformer = new CustomEntityTransformer(initializeXMLInputFactory());
    this.registeredNamespaces = new HashMap<>();
    this.registeredNamespaces.put(defaultNamespace().eipNamespace(), defaultNamespace());
    requiredNamespaces().forEach(s -> this.registeredNamespaces.put(s.eipNamespace(), s));
    namespaceSpecs.forEach(s -> this.registeredNamespaces.put(s.eipNamespace(), s));

    this.xmlToEipNamespaceMap =
        registeredNamespaces.values().stream()
            .collect(Collectors.toMap(NamespaceSpec::xmlNamespace, NamespaceSpec::eipNamespace));
  }

  private void validatePrefixes(Collection<NamespaceSpec> namespaceSpecs) {
    for (NamespaceSpec ns : namespaceSpecs) {
      if (this.reservedPrefixes.contains(ns.eipNamespace())) {
        throw new IllegalArgumentException(
            String.format("'%s' is a reserved namespace prefix", ns.eipNamespace()));
      }
    }
  }

  /**
   * Transform an {@link EipGraph} instance to an XML document
   *
   * @param graph input graph
   * @param output where the output XML will be written to
   * @param customEntities user-defined entities to be inlined in the output
   * @return An empty list for a successful transformation, otherwise a non-empty list of {@link
   *     TransformationError} is returned.
   * @throws TransformerException thrown if a critical error preventing the transformation is
   *     encountered
   */
  public final List<TransformationError> toXml(
      EipGraph graph, Writer output, Map<String, String> customEntities)
      throws TransformerException {
    List<TransformationError> errors = new ArrayList<>();
    try {
      XMLEventWriter writer = outputFactory.createXMLEventWriter(output);
      writer.setDefaultNamespace(defaultNamespace().xmlNamespace());

      writer.add(eventFactory.createStartDocument());

      StartElement root = createRootElement(graph);
      writer.add(root);

      errors.addAll(customEntityTransformer.apply(customEntities, writer));

      errors.addAll(writeNodes(graph, writer));

      writer.add(eventFactory.createEndElement(root.getName(), null));

      writer.add(eventFactory.createEndDocument());

      writer.flush();
      writer.close();
    } catch (XMLStreamException | RuntimeException e) {
      throw new TransformerException(e);
    }
    return errors;
  }

  /**
   * Transform an {@link EipGraph} instance to an XML document
   *
   * @param graph input graph
   * @param output where the output XML will be written to
   * @return An empty list for a successful transformation, otherwise a non-empty list of {@link
   *     TransformationError} is returned.
   * @throws TransformerException thrown if a critical error preventing the transformation is
   *     encountered
   */
  public final List<TransformationError> toXml(EipGraph graph, Writer output)
      throws TransformerException {
    return toXml(graph, output, Collections.emptyMap());
  }

  public final XmlTranslationOutput fromXml(Reader xml, XMLValidationSchema schema)
      throws TransformerException {
    try {
      // XmlEventReader does not support validation while parsing. Use XmlStreamReader instead.
      XMLStreamReader2 streamReader = (XMLStreamReader2) inputFactory.createXMLStreamReader(xml);
      if (schema != null) {
        streamReader.validateAgainst(schema);
      }

      XMLStreamReader reader = inputFactory.createFilteredReader(streamReader, this::elementFilter);

      XmlTransformer xmlTransformer = nodeTransformerFactory.getXmlTransformer();
      List<XmlElement> elements = new ArrayList<>();

      while (reader.hasNext()) {
        reader.next();
        if (reader.isStartElement()) {
          // TODO: Validate id attribute
          XmlElement element = createElement(reader);
          parseChildren(reader, element, 1);
          elements.add(element);
        }
      }
    } catch (XMLStreamException e) {
      throw new TransformerException(e);
    }

    return null;
  }

  private void parseChildren(XMLStreamReader reader, XmlElement parent, int depth)
      throws TransformerException, XMLStreamException {

    while (reader.hasNext() && depth > 0) {
      reader.next();
      if (reader.isStartElement()) {
        XmlElement currElement = createElement(reader);
        if (parent != null) {
          parent.children().add(currElement);
        }
        parseChildren(reader, currElement, depth + 1);
        return;
      }
      if (reader.isEndElement()) {
        parseChildren(reader, parent, depth - 1);
        return;
      }
    }
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
    return reader.isStartElement() || reader.isEndElement();
  }

  protected abstract NamespaceSpec defaultNamespace();

  protected abstract Set<NamespaceSpec> requiredNamespaces();

  protected abstract QName rootElement();

  private NodeTransformer getNodeTransformer(EipId id) {
    return this.nodeTransformerFactory.getNodeTransformer(id);
  }

  private StartElement createRootElement(EipGraph graph) {
    List<String> eipNamespaces = collectEipNamespaces(graph);
    return eventFactory.createStartElement(
        rootElement(), getRootAttributes(eipNamespaces), getRootNamespaces(eipNamespaces));
  }

  /**
   * Does a first pass through the graph to collect all the included namespaces up-front, in order
   * to define them on the root element. If a node with an unregistered EIP namespace is
   * encountered, an exception is immediately thrown.
   *
   * <p>This approach forces us to do an extra traversal of the graph, so it might prove too
   * inefficient.
   */
  private List<String> collectEipNamespaces(EipGraph graph) {
    return graph
        .traverse()
        .filter(n -> !this.reservedPrefixes.contains(n.eipId().namespace()))
        .map(
            n -> {
              String ns = n.eipId().namespace();
              if (!this.registeredNamespaces.containsKey(ns)) {
                throw new IllegalArgumentException(String.format("Unregistered namespace: %s", ns));
              }
              return ns;
            })
        .distinct()
        .toList();
  }

  private Iterator<Attribute> getRootAttributes(List<String> eipNamespaces) {
    Stream<String> defaultNamespaceLocation =
        Stream.of(defaultNamespace().xmlNamespace(), defaultNamespace().schemaLocation());
    Stream<String> requiredNamespaceLocations =
        requiredNamespaces().stream()
            .flatMap(spec -> Stream.of(spec.xmlNamespace(), spec.schemaLocation()));
    Stream<String> collectedLocations =
        eipNamespaces.stream().flatMap(ns -> Stream.of(getXmlNamespace(ns), getSchemaLocation(ns)));

    // TODO: Figure out how to safely use a newline inside an attribute
    String locString =
        Stream.of(defaultNamespaceLocation, requiredNamespaceLocations, collectedLocations)
            .flatMap(Function.identity())
            .collect(Collectors.joining(" "));

    return List.of(
            eventFactory.createAttribute(
                XSI_PREFIX,
                XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                "schemaLocation",
                locString))
        .iterator();
  }

  private Iterator<Namespace> getRootNamespaces(List<String> eipNamespaces) {
    Stream<Namespace> defaultNamespace =
        Stream.of(this.eventFactory.createNamespace(defaultNamespace().xmlNamespace()));
    Stream<Namespace> xsiNamespace =
        Stream.of(
            this.eventFactory.createNamespace(
                XSI_PREFIX, XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI));
    Stream<Namespace> requiredNamespaces =
        requiredNamespaces().stream()
            .map(
                spec ->
                    this.eventFactory.createNamespace(spec.eipNamespace(), spec.xmlNamespace()));
    Stream<Namespace> collectedNamespaces =
        eipNamespaces.stream()
            .map(ns -> this.eventFactory.createNamespace(ns, getXmlNamespace(ns)));
    return Stream.of(defaultNamespace, xsiNamespace, requiredNamespaces, collectedNamespaces)
        .flatMap(Function.identity())
        .iterator();
  }

  private List<TransformationError> writeNodes(EipGraph graph, XMLEventWriter writer) {
    List<TransformationError> errors = new ArrayList<>();

    // Using a for-each loop rather than stream operations due to the checked exception.
    // If this approach proves inefficient, an alternative is to define our own ErrorListener
    // interface that throws runtime exceptions.
    for (EipNode node : graph.traverse().toList()) {
      try {
        NodeTransformer transformer = getNodeTransformer(node.eipId());
        List<XmlElement> elements = transformer.apply(node, graph);
        elements.forEach(e -> writeElement(e, writer));
      } catch (RuntimeException e) {
        TransformationError error = new TransformationError(node.id(), new TransformerException(e));
        errors.add(error);
      }
    }
    return errors;
  }

  private void writeElement(XmlElement element, XMLEventWriter writer) {
    try {
      writer.add(
          this.eventFactory.createStartElement(
              element.prefix(),
              getXmlNamespace(element.prefix()),
              element.localName(),
              attributeIterator(element.attributes()),
              null));

      for (XmlElement c : element.children()) {
        writeElement(c, writer);
      }

      writer.add(
          this.eventFactory.createEndElement(
              element.prefix(), getXmlNamespace(element.prefix()), element.localName()));
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  private String getXmlNamespace(String eipNamespace) {
    NamespaceSpec spec = this.registeredNamespaces.get(eipNamespace);
    return spec == null ? null : spec.xmlNamespace();
  }

  private String getSchemaLocation(String eipNamespace) {
    NamespaceSpec spec = this.registeredNamespaces.get(eipNamespace);
    return spec == null ? null : spec.schemaLocation();
  }

  private Iterator<Attribute> attributeIterator(Map<String, Object> attributes) {
    return attributes.entrySet().stream()
        .map(e -> this.eventFactory.createAttribute(e.getKey(), e.getValue().toString()))
        .iterator();
  }

  private Set<String> collectReservedPrefixes() {
    Stream<String> requiredPrefixes =
        requiredNamespaces().stream().map(NamespaceSpec::eipNamespace);
    return Stream.concat(
            Stream.of(XML_NS_PREFIX, XSI_PREFIX, defaultNamespace().eipNamespace()),
            requiredPrefixes)
        .collect(Collectors.toUnmodifiableSet());
  }

  static XMLInputFactory initializeXMLInputFactory() {
    XMLInputFactory factory = WstxInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
