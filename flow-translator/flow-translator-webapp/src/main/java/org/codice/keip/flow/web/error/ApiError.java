package org.codice.keip.flow.web.error;

import java.util.Collections;
import java.util.List;
import org.springframework.core.NestedExceptionUtils;

public record ApiError<T>(String message, String type, List<T> details) {

  public static <T> ApiError<T> of(Exception ex) {
    return of(ex, Collections.emptyList());
  }

  public static <T> ApiError<T> of(Exception ex, List<T> details) {
    Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
    return new ApiError<>(root.getMessage(), root.getClass().getSimpleName(), details);
  }
}
