package org.codice.keip.schemas.validation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.JsonSchemaFactory.Builder;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.resource.ClasspathSchemaLoader;
import com.networknt.schema.resource.DisallowSchemaLoader;
import com.networknt.schema.resource.SchemaMapper;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.stream.Collectors;

/** Validates a JSON against a defined EIP JSON schema. */
public final class EipSchemaValidator {

  private ObjectMapper jsonMapper =
      JsonMapper.builder()
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();
  private final JsonSchema eipSchema;
  static final String SCHEMAS_PREFIX = "https://github.com/codice/keip-canvas/schemas/";

  private EipSchemaValidator(String schemaPath) {
    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.getInstance(VersionFlag.V7, this::configureFactoryBuilder);

    SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().failFast(true).build();

    this.eipSchema =
        jsonSchemaFactory.getSchema(SchemaLocation.of(SCHEMAS_PREFIX + schemaPath), config);

    // Eagerly check for $ref resolution errors
    this.eipSchema.initializeValidators();
  }

  public static EipSchemaValidator getInstance(EipSchema schema) {
    return switch (schema) {
      case FLOW -> new EipSchemaValidator("eipFlow.schema.json");
      case COMPONENT_DEFINITION -> new EipSchemaValidator("eipComponentDef.schema.json");
    };
  }

  public void setJsonMapper(ObjectMapper mapper) {
    this.jsonMapper = mapper;
  }

  /**
   * Validates an EIP JSON. By default, the validation will fail fast (return when it encounters its
   * first error).
   *
   * @param json the EIP JSON
   * @return a collection of validation error messages. For a successful validation, an empty
   *     collection is returned.
   */
  public Set<String> validate(Reader json) {
    JsonNode jsonNode = toJsonNode(json);
    return toStrings(this.eipSchema.validate(jsonNode));
  }

  /**
   * Validates an EIP JSON.
   *
   * @param json the EIP JSON
   * @param failFast if true, validation stops when the first error is encountered. Otherwise, the
   *     validation continues even in the presence of errors.
   * @return a collection of validation error messages. For a successful validation, an empty
   *     collection is returned.
   */
  public Set<String> validate(Reader json, boolean failFast) {
    JsonNode jsonNode = toJsonNode(json);
    return toStrings(
        this.eipSchema.validate(
            jsonNode, executionContext -> executionContext.setFailFast(failFast)));
  }

  private JsonNode toJsonNode(Reader r) {
    try {
      return jsonMapper.readTree(r);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<String> toStrings(Set<ValidationMessage> validationMessages) {
    return validationMessages.stream()
        .map(ValidationMessage::getMessage)
        .collect(Collectors.toSet());
  }

  /**
   * Configures the jsonSchemaFactory to use a version-less schema mapper and restricts the schema
   * retriever to the classpath (network requests are disabled).
   */
  private void configureFactoryBuilder(Builder builder) {
    builder
        .schemaMappers(sm -> sm.add(new VersionlessSchemaMapper()))
        .schemaLoaders(
            sl -> sl.add(new ClasspathSchemaLoader()).add(DisallowSchemaLoader.getInstance()));
  }

  /**
   * Enables loading JSON schemas from the classpath without needing to specify an explicit schema
   * version. Consequently, schema versions can be controlled solely through dependency management,
   * without requiring any code changes. However, this approach does restrict the validator to using
   * a single schema version.
   */
  static class VersionlessSchemaMapper implements SchemaMapper {
    @Override
    public AbsoluteIri map(AbsoluteIri absoluteIri) {
      String iri = absoluteIri.toString();
      if (iri.startsWith(SCHEMAS_PREFIX) && iri.endsWith(".json")) {
        String[] parts = iri.split("/");
        return AbsoluteIri.of("classpath:schemas/").resolve(parts[parts.length - 1]);
      }
      return null;
    }
  }
}
