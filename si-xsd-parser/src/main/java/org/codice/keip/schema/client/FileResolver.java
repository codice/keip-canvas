package org.codice.keip.schema.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

public class FileResolver implements UriResolver {

  public InputStream fetchUri(URI uri) throws Exception {
    File xsd;
    if (uri.getPath() != null) {
      xsd = new File(uri);
    } else {
      // Handles relative file path URIs (e.g. "file:tmp/local")
      // Note: this type of file URI is not part of the spec, but is useful for
      // enabling access to project repo specific files.
      xsd = new File(uri.getSchemeSpecificPart());
    }

    if (!xsd.isFile()) {
      throw new IllegalArgumentException("No XSD file found at " + xsd.getAbsolutePath());
    }
    return new BufferedInputStream(new FileInputStream(xsd));
  }
}
