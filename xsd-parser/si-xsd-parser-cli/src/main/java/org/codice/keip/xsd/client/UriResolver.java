package org.codice.keip.xsd.client;

import java.io.InputStream;
import java.net.URI;

public interface UriResolver {

  /** Resolve the provided URI into an InputStream of the target resource. */
  InputStream fetchUri(URI uri) throws Exception;
}
