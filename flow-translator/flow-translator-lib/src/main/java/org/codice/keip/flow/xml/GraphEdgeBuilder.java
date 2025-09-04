package org.codice.keip.flow.xml;

import java.util.Collection;
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.model.EipGraph;
import org.codice.keip.flow.model.EipNode;

/**
 * When parsing an XML document, each {@link XmlElement} is first transformed into an {@link
 * EipNode}. Implementations of this interface should iterate over the parsed nodes and extract the
 * connections required to build a node graph.
 */
@FunctionalInterface
public interface GraphEdgeBuilder {
  EipGraph toGraph(Collection<EipNode> nodes) throws TransformerException;
}
