package org.codice.keip.flow.xml.spring;

import org.codice.keip.flow.model.EipNode;
import org.codice.keip.flow.xml.XmlElement;
import org.codice.keip.flow.xml.XmlTransformer;

/** A default implementation for generating an {@link EipNode} from an {@link XmlElement} */
public class DefaultXmlTransformer implements XmlTransformer {

  @Override
  public EipNode apply(XmlElement element) {
    return null;
  }
}
