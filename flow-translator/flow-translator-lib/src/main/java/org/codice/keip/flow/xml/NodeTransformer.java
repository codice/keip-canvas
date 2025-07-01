package org.codice.keip.flow.xml;

import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;
import java.util.List;

/**
 * Implementations of this interface transform an {@link EipNode} into one or more {@link
 * XmlElement}. The transformed elements are then composed together to generate the output XML. The
 * {@link EipGraph} containing the node is also provided as context to the transformer.
 */
@FunctionalInterface
public interface NodeTransformer {

  List<XmlElement> apply(EipNode node, EipGraph graph);
}
