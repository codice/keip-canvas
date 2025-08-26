package org.codice.keip.flow.xml;

import java.util.List;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.model.EipGraph;

public record XmlTranslationOutput(EipGraph graph, List<TransformationError> errors) {}
