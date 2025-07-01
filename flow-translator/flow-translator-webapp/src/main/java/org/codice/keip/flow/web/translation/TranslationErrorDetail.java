package org.codice.keip.flow.web.translation;

import org.codice.keip.flow.error.TransformationError;
import org.springframework.core.NestedExceptionUtils;

record TranslationErrorDetail(String nodeId, String message) {

  static TranslationErrorDetail from(TransformationError error) {
    return new TranslationErrorDetail(
        error.source(), NestedExceptionUtils.getMostSpecificCause(error.exception()).getMessage());
  }
}
