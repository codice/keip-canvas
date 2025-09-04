package org.codice.keip.flow.xml;

import java.util.List;
import org.codice.keip.flow.error.TransformationError;

public record TranslationResult<T>(T result, List<TransformationError> errors) {}
