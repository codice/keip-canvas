package org.codice.keip.flow.xml;

import static javax.xml.XMLConstants.XML_NS_PREFIX;

import com.ctc.wstx.stax.WstxEventFactory;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms an intermediate {@link EipGraph} representation to an XML document. This base class
 * takes care of the general transformation process, to create XML targeting specific platforms,
 * extend this class and register specialized {@link NodeTransformer}s.
 */
public abstract class GraphXmlSerializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphXmlSerializer.class);
  private static final String XSI_PREFIX = "xsi";

  private final XMLEventFactory eventFactory = WstxEventFactory.newFactory();
  private final XMLOutputFactory outputFactory = WstxOutputFactory.newFactory();
  private final XmlElementWriter elementWriter = new XmlElementWriter(eventFactory);
  private final Set<String> reservedPrefixes = collectReservedPrefixes();

  private final CustomEntityTransformer customEntityTransformer;

  // maps an eipNamespace to a NamespaceSpec
  private final Map<String, NamespaceSpec> registeredNamespaces;

  protected GraphXmlSerializer(Collection<NamespaceSpec> namespaceSpecs) {
    Collection<NamespaceSpec> filteredNamespaces = excludeReservedNamespaces(namespaceSpecs);
    this.customEntityTransformer = new CustomEntityTransformer(initializeXMLInputFactory());
    this.registeredNamespaces = buildRegisteredNamespaceMap(filteredNamespaces);
  }

  protected abstract NamespaceSpec defaultNamespace();

  protected abstract Set<NamespaceSpec> requiredNamespaces();

  protected abstract QName rootElement();

  protected abstract NodeTransformer getNodeTransformer();

  private Map<String, NamespaceSpec> buildRegisteredNamespaceMap(
      Collection<NamespaceSpec> namespaceSpecs) {
    Map<String, NamespaceSpec> map = new HashMap<>();
    map.put(defaultNamespace().eipNamespace(), defaultNamespace());
    requiredNamespaces().forEach(s -> map.put(s.eipNamespace(), s));
    namespaceSpecs.forEach(s -> map.put(s.eipNamespace(), s));
    return Collections.unmodifiableMap(map);
  }

  private List<NamespaceSpec> excludeReservedNamespaces(Collection<NamespaceSpec> namespaceSpecs) {
    List<NamespaceSpec> filtered = new ArrayList<>();
    for (NamespaceSpec ns : namespaceSpecs) {
      if (this.reservedPrefixes.contains(ns.eipNamespace())) {
        LOGGER.warn(
            "'{}' is a reserved namespace prefix. The provided namespace entry will be ignored",
            ns.eipNamespace());
      } else {
        filtered.add(ns);
      }
    }
    return filtered;
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
        List<XmlElement> elements = getNodeTransformer().apply(node, graph);
        elements.forEach(e -> elementWriter.write(e, writer));
      } catch (RuntimeException e) {
        TransformationError error = new TransformationError(node.id(), new TransformerException(e));
        errors.add(error);
      }
    }
    return errors;
  }

  private String getXmlNamespace(String eipNamespace) {
    NamespaceSpec spec = this.registeredNamespaces.get(eipNamespace);
    return spec == null ? null : spec.xmlNamespace();
  }

  private String getSchemaLocation(String eipNamespace) {
    NamespaceSpec spec = this.registeredNamespaces.get(eipNamespace);
    return spec == null ? null : spec.schemaLocation();
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
