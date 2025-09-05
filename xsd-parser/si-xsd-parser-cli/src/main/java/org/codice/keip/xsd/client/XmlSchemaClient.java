package org.codice.keip.xsd.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlSchemaClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlSchemaClient.class);

  private final Map<String, URI> customSchemaLocations;

  private final Map<String, UriResolver> uriResolvers = new HashMap<>();

  public XmlSchemaClient(Map<String, URI> customSchemaLocations) {
    this.customSchemaLocations = customSchemaLocations;
    this.registerUriResolver(new FileResolver(), "file");
    this.registerUriResolver(new HttpResolver(), "http", "https");
    this.registerUriResolver(new ClasspathResolver(), "classpath");
  }

  public XmlSchemaCollection collect(URI schemaLocation) throws IOException, InterruptedException {
    LOGGER.info("Fetching target schema xml at: {}", schemaLocation);

    UriResolver uriResolver = getUriResolver(schemaLocation.getScheme());

    try (var inputStream = uriResolver.fetchUri(schemaLocation)) {
      Document doc = toDomDocument(inputStream);
      overwriteSchemaLocations(doc);
      var schemaCollection = new XmlSchemaCollection();
      schemaCollection.read(doc);
      return schemaCollection;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Document toDomDocument(InputStream xmlStream)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder builder = dbf.newDocumentBuilder();
    return builder.parse(xmlStream);
  }

  /**
   * Search all import elements in the schema and update any namespaces found in the {@link
   * #customSchemaLocations} map to point to the custom schemaLocation.
   */
  private void overwriteSchemaLocations(Document doc) {
    NodeList importedNodes =
        doc.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "import");
    for (int i = 0; i < importedNodes.getLength(); i++) {
      Node node = importedNodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element el = (Element) node;
        URI schemaLocation = this.customSchemaLocations.get(el.getAttribute("namespace"));
        if (schemaLocation != null) {
          el.setAttribute("schemaLocation", schemaLocation.toString());
        }
      }
    }
  }

  void registerUriResolver(UriResolver uriResolver, String... schemes) {
    for (String scheme : schemes) {
      this.uriResolvers.put(scheme, uriResolver);
    }
  }

  private UriResolver getUriResolver(String scheme) throws IllegalArgumentException {
    if (!this.uriResolvers.containsKey(scheme)) {
      throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
    }
    return this.uriResolvers.get(scheme);
  }
}
