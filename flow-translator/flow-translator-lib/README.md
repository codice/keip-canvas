# EIP Flow Translator

A library for translating
an [EIP Flow JSON](/schemas/model/json/eipFlow.schema.json) into
a runnable integration framework target (e.g. Spring Integration XML).

## Installation

### Maven

To build and install the project locally (requires Java 21 and Maven 3.9+):

```shell
mvn install
```

To use, add as a dependency in `pom.xml`:

```xml

<dependency>
    <groupId>org.codice.keip</groupId>
    <artifactId>flow-translator-lib</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

```java
import org.codice.keip.flow.FlowTranslator;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.xml.GraphTransformer;
import org.codice.keip.flow.xml.spring.IntegrationGraphTransformer;

// Specify a translation target by initializing a GraphTransformer implementation
// e.g. for Spring Integration XML:
GraphTransformer intTransformer = new IntegrationGraphTransformer(namespaceSpecs);

// Initialize top-level translator and pass in target transformer
FlowTranslator translator = new FlowTranslator(intTransformer);

// Optionally, register custom node transformers
translator.registerNodeTransformer(id, nodeTransformer);

// Translate flow to xml
List<TransformationError> errors = translator.toXml(flow, output);
```

## Development

For more details on the library's design and potential extension points, see [the architecture docs](./ARCHITECTURE.md).