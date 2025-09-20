package org.codice.keip.flow.web.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.codice.keip.flow.xml.NamespaceSpec;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class SchemaBuilder {

  private static final List<String> INIT_SCHEMA_URIS =
      List.of(
          "http://www.springframework.org/schema/beans/spring-beans.xsd",
          "https://www.springframework.org/schema/beans/spring-beans.xsd",
          "http://www.springframework.org/schema/tool/spring-tool.xsd",
          "https://www.springframework.org/schema/tool/spring-tool.xsd");

  private static final String CLASSPATH_PREFIX = "classpath:";

  static Schema buildSpringIntegrationSchemas(List<NamespaceSpec> namespaceSpecs)
      throws IOException, SAXException {

    List<InputStream> springIntegrationSchemas = new ArrayList<>();
    try {

      for (NamespaceSpec spec : namespaceSpecs) {
        if (!spec.schemaLocation().startsWith(CLASSPATH_PREFIX)) {
          continue;
        }

        InputStream is =
            SchemaBuilder.class
                .getClassLoader()
                .getResourceAsStream(spec.schemaLocation().substring(CLASSPATH_PREFIX.length()));

        if (is != null) {
          springIntegrationSchemas.add(is);
        }
      }

      List<Source> schemaSources = buildSpringBeansSchemas();
      schemaSources.addAll(springIntegrationSchemas.stream().map(StreamSource::new).toList());
      Source[] srcArr = schemaSources.toArray(new Source[0]);
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      schemaFactory.setResourceResolver(new ClasspathLSResourceResolver());
      return schemaFactory.newSchema(srcArr);
    } finally {
      for (InputStream is : springIntegrationSchemas) {
        is.close();
      }
    }
  }

  private static List<Source> buildSpringBeansSchemas() throws IOException {
    PluggableSchemaResolver resolver =
        new PluggableSchemaResolver(SchemaBuilder.class.getClassLoader());

    List<Source> sources = new ArrayList<>();
    for (String schemaLocation : INIT_SCHEMA_URIS) {
      InputSource inputSource = resolver.resolveEntity(null, schemaLocation);
      if (inputSource != null) {
        sources.add(new StreamSource(inputSource.getByteStream()));
      }
    }

    return sources;
  }

  // Workaround for groovy xsd including the scripting-core xsd with a schemaLocation only
  // (no namespaceUri)
  private static class ClasspathLSResourceResolver implements LSResourceResolver {

    @Override
    public LSInput resolveResource(
        String type, String namespaceURI, String publicId, String systemId, String baseURI) {
      if (systemId != null
          && systemId.equals(
              "https://www.springframework.org/schema/integration/scripting/spring-integration-scripting-core.xsd")) {
        InputStream resource =
            SchemaBuilder.class
                .getClassLoader()
                .getResourceAsStream(
                    "org/springframework/integration/scripting/config/spring-integration-scripting-core.xsd");
        if (resource != null) {
          // Does the SchemaFactory close the 'resource' InputStream?
          return new ClasspathLSInput(publicId, systemId, resource);
        }
      }
      return null;
    }
  }

  private static class ClasspathLSInput implements LSInput {
    private String publicId;
    private String systemId;
    private InputStream byteStream;

    public ClasspathLSInput(String publicId, String systemId, InputStream byteStream) {
      this.publicId = publicId;
      this.systemId = systemId;
      this.byteStream = byteStream;
    }

    @Override
    public Reader getCharacterStream() {
      return null;
    }

    @Override
    public void setCharacterStream(Reader characterStream) {}

    @Override
    public InputStream getByteStream() {
      return byteStream;
    }

    @Override
    public void setByteStream(InputStream byteStream) {
      this.byteStream = byteStream;
    }

    @Override
    public String getStringData() {
      return null;
    }

    @Override
    public void setStringData(String stringData) {}

    @Override
    public String getSystemId() {
      return systemId;
    }

    @Override
    public void setSystemId(String systemId) {
      this.systemId = systemId;
    }

    @Override
    public String getPublicId() {
      return publicId;
    }

    @Override
    public void setPublicId(String publicId) {
      this.publicId = publicId;
    }

    @Override
    public String getBaseURI() {
      return null;
    }

    @Override
    public void setBaseURI(String baseURI) {}

    @Override
    public String getEncoding() {
      return null;
    }

    @Override
    public void setEncoding(String encoding) {}

    @Override
    public boolean getCertifiedText() {
      return false;
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {}
  }
}
