package org.codice.keip.schema.config;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class XsdSourceConfiguration {

  private List<SchemaIdentifier> schemas;

  private List<SchemaIdentifier> importedSchemaLocations;

  public List<SchemaIdentifier> getSchemas() {
    return schemas;
  }

  public void setSchemas(List<SchemaIdentifier> schemas) {
    this.schemas = schemas;
  }

  List<SchemaIdentifier> getImportedSchemaLocations() {
    return importedSchemaLocations;
  }

  public Map<String, URI> getImportedSchemaLocationsMap() {
    return importedSchemaLocations.stream()
        .collect(Collectors.toMap(SchemaIdentifier::getNamespace, SchemaIdentifier::getLocation));
  }

  public void setImportedSchemaLocations(List<SchemaIdentifier> importedSchemaLocations) {
    this.importedSchemaLocations = importedSchemaLocations;
  }

  public static XsdSourceConfiguration readYaml(InputStream is) {
    Yaml yaml = new Yaml(new Constructor(XsdSourceConfiguration.class, new LoaderOptions()));
    return yaml.loadAs(is, XsdSourceConfiguration.class);
  }

  public static class SchemaIdentifier {
    private String alias;
    private String namespace;
    private URI location;

    private Set<String> excludedElements = Collections.emptySet();

    public String getAlias() {
      return alias;
    }

    public String getNamespace() {
      return namespace;
    }

    public URI getLocation() {
      return location;
    }

    public Set<String> getExcludedElements() {
      return excludedElements;
    }

    public void setAlias(String alias) {
      this.alias = alias;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public void setLocation(URI location) {
      this.location = location;
    }

    public void setExcludedElements(Set<String> excludedElements) {
      this.excludedElements = excludedElements;
    }
  }
}
