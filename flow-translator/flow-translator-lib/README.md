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
import org.codice.keip.flow.xml.GraphXmlSerializer;
import org.codice.keip.flow.xml.spring.IntegrationGraphXmlSerializer;

// Specify a translation target by initializing a GraphXmlSerializer implementation
// e.g. for Spring Integration XML:
GraphXmlSerializer serializer = new IntegrationGraphXmlSerializer(namespaceSpecs);

// Initialize top-level translator
FlowTranslator translator = new FlowTranslator(serializer);

// Translate flow to xml
List<TransformationError> errors = translator.toXml(flow, output);
```

## Development

For more details on the library's design and potential extension points, see [the architecture docs](./ARCHITECTURE.md).