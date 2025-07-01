package org.codice.keip.flow.xml;

import java.util.List;
import java.util.Map;

public record XmlElement(
    String prefix, String localName, Map<String, Object> attributes, List<XmlElement> children) {}
