# EIP JSON Schemas

### Overview

This module contains the JSON schemas used to model the data structures shared across Keip Canvas components.
The schemas can be used to enforce a contract between components by leveraging schema validation and
code-generation tools.

The EIP schemas are stored under `model/json/`, with some examples under `model/json/examples`.

### Versioning

The version of the schemas are stored as part of each schema's URI set with the `$id` keyword. Example:

```
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://github.com/codice/keip-canvas/schemas/v0.1.0/eipFlow.schema.json",
  "title": "EipFlow",
  .
  .
  .
}
```

Whenever the schemas are modified, the version number should be updated according
to [Semantic Versioning](https://semver.org/) guidelines.

### JVM services

For JVM-based services, a Maven dependency is provided to streamline validating JSONs against the schema:

```xml
<dependency>
    <groupId>org.codice.keip.schemas</groupId>
    <artifactId>validation</artifactId>
    <version>0.1.0</version>
</dependency>
```

The `EipSchemaValidator` can then be used:

```java
import org.codice.keip.schemas.validation.EipSchema;
import org.codice.keip.schemas.validation.EipSchemaValidator;
        
EipSchemaValidator validator = EipSchemaValidator.getInstance(EipSchema.FLOW);

// errors will be empty if validation is successful
Set<String> errors = validator.validate(jsonReader);
```