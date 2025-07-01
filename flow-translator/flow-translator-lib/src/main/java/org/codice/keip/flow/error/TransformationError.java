package org.codice.keip.flow.error;

public record TransformationError(String source, Exception exception) {}
