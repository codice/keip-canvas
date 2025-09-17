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
import org.codice.keip.flow.xml.TranslationResult;

/** Transforms an EIP {@link Flow} to an XML document. */
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
   * @param flow The flow input to transform.
   * @param outputXml The result of the transformation.
   * @return a collection of transformation error messages. An empty collection is returned for a
   *     successful transformation.
   * @throws TransformerException thrown only if an unrecoverable error occurs, otherwise errors are
   *     collected and returned once transformation is complete.
   */
  public List<TransformationError> toXml(Flow flow, Writer outputXml) throws TransformerException {
    if (this.graphXmlSerializer == null) {
      throw new UnsupportedOperationException(
          "A GraphXmlSerializer must be initialized before calling 'toXml'");
    }

    EipGraph graph = GuavaGraph.from(flow);
    return graphXmlSerializer.toXml(graph, outputXml, flow.customEntities());
  }

  public TranslationResult<Flow> fromXml(InputStream xml) throws TransformerException {
    if (this.graphXmlParser == null) {
      throw new UnsupportedOperationException(
          "A GraphXmlParser must be initialized before calling 'fromXml'");
    }

    XmlParseResult result = graphXmlParser.fromXml(xml);
    Flow flow = result.graph().toFlow();
    flow = new Flow(flow.nodes(), flow.edges(), result.customEntities());
    return new TranslationResult<>(flow, result.errors());
  }
}
