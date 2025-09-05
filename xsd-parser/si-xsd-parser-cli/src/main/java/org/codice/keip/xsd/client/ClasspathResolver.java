package org.codice.keip.xsd.client;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;

public class ClasspathResolver implements UriResolver {
  @Override
  public InputStream fetchUri(URI uri) {
    String path = uri.getSchemeSpecificPart();
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
    if (inputStream == null) {
      throw new IllegalArgumentException(
          String.format("No XSD file found on the classpath at '%s'", path));
    }

    return new BufferedInputStream(inputStream);
  }
}
