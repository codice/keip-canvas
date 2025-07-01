package org.codice.keip.flow.web.error;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<DefaultErrorResponse> malformedInput(HttpMessageNotReadableException ex) {
    LOGGER.error("Malformed Input", ex);
    return buildResponse(ApiError.of(new IOException(ex.getMessage())), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<DefaultErrorResponse> illegalArgument(IllegalArgumentException ex) {
    LOGGER.error("Error processing request", ex);
    return buildResponse(ApiError.of(ex), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<DefaultErrorResponse> defaultExceptionHandler(Exception ex) {
    LOGGER.error("Error processing request", ex);
    return buildResponse(ApiError.of(ex), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public ResponseEntity<DefaultErrorResponse> buildResponse(
      ApiError<Object> error, HttpStatus status) {
    return ResponseEntity.status(status).body(new DefaultErrorResponse(error));
  }
}
