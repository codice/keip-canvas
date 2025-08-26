package org.codice.keip.flow.xml;

import javax.xml.transform.TransformerException;
import org.codice.keip.flow.ComponentRegistry;
import org.codice.keip.flow.model.EipNode;

/** Implementations of this interface transform an {@link XmlElement} into an {@link EipNode} */
@FunctionalInterface
public interface XmlElementTransformer {
  EipNode apply(XmlElement element, ComponentRegistry registry) throws TransformerException;
}
