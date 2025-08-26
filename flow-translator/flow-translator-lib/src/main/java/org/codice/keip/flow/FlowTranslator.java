package org.codice.keip.flow;

import java.io.Writer;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.graph.GuavaGraph;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.Flow;
import org.codice.keip.flow.xml.GraphTransformer;

/** Transforms an EIP {@link Flow} to an XML document. */
public final class FlowTranslator {

  private final GraphTransformer graphTransformer;

  public FlowTranslator(GraphTransformer graphTransformer) {
    this.graphTransformer = graphTransformer;
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
    EipGraph graph = GuavaGraph.from(flow);
    return graphTransformer.toXml(graph, outputXml, flow.customEntities());
  }
}
