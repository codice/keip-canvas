package org.codice.keip.flow.xml;

import javax.xml.transform.TransformerException;
import org.codice.keip.flow.model.EipNode;

/** Implementations of this interface transform an {@link XmlElement} into an {@link EipNode} */
@FunctionalInterface
public interface XmlTransformer {
  EipNode apply(XmlElement element) throws TransformerException;
}
