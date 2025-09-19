package org.codice.keip.flow;

import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.graph.GuavaGraph;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.Flow;
import org.codice.keip.flow.xml.GraphXmlParser;
import org.codice.keip.flow.xml.GraphXmlParser.XmlParseResult;
import org.codice.keip.flow.xml.GraphXmlSerializer;

/**
 * Translates between an EIP {@link Flow} and its XML representations.
 *
 * <p>This class acts as a bridge between the flow model and the XML format used to implement the
 * patterns. It relies on:
 *
 * <ul>
 *   <li>{@link GraphXmlSerializer} to serialize a flow into XML.
 *   <li>{@link GraphXmlParser} to parse XML into a flow.
 * </ul>
 *
 * <h3>Thread Safety</h3>
 *
 * <p>Both {@link GraphXmlSerializer} and {@link GraphXmlParser} are thread-safe. As a result,
 * {@code FlowTranslator} is also thread-safe and can be safely reused across threads without
 * external synchronization.
 */
public final class FlowTranslator {

  private final GraphXmlSerializer graphXmlSerializer;
  private final GraphXmlParser graphXmlParser;

  public FlowTranslator(GraphXmlSerializer graphXmlSerializer, GraphXmlParser graphXmlParser) {
    this.graphXmlSerializer = graphXmlSerializer;
    this.graphXmlParser = graphXmlParser;
  }

  public FlowTranslator(GraphXmlSerializer graphXmlSerializer) {
    this.graphXmlSerializer = graphXmlSerializer;
    this.graphXmlParser = null;
  }

  public FlowTranslator(GraphXmlParser graphXmlParser) {
    this.graphXmlParser = graphXmlParser;
    this.graphXmlSerializer = null;
  }

  /**
   * Converts a {@link Flow} into its XML representation.
   *
   * @param flow The flow input to serialize
   * @param outputXml The result of the transformation
   * @return a collection of transformation error messages. An empty collection is returned for a
   *     successful transformation.
   * @throws TransformerException thrown only if an unrecoverable error occurs, otherwise errors are
   *     collected and returned once transformation is complete.
   * @throws UnsupportedOperationException if this instance was not constructed with a {@link
   *     GraphXmlSerializer}
   */
  public List<TransformationError> toXml(Flow flow, Writer outputXml) throws TransformerException {
    if (this.graphXmlSerializer == null) {
      throw new UnsupportedOperationException(
          "A GraphXmlSerializer must be initialized before calling 'toXml'");
    }

    EipGraph graph = GuavaGraph.from(flow);
    return graphXmlSerializer.toXml(graph, outputXml, flow.customEntities());
  }

  /**
   * Parses an XML document into a {@link Flow}.
   *
   * @param xml the XML input stream to parse
   * @return the parsed {@link Flow} object
   * @throws TransformerException if a parsing or transformation error occurs
   * @throws UnsupportedOperationException if this instance was not constructed with a {@link
   *     GraphXmlParser}
   */
  public Flow fromXml(InputStream xml) throws TransformerException {
    if (this.graphXmlParser == null) {
      throw new UnsupportedOperationException(
          "A GraphXmlParser must be initialized before calling 'fromXml'");
    }

    XmlParseResult result = graphXmlParser.fromXml(xml);
    Flow flow = result.graph().toFlow();
    return new Flow(flow.nodes(), flow.edges(), result.customEntities());
  }
}
