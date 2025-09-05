# Spring Integration XSD Parser

A tool for parsing Spring Integration XSDs into
an [Enterprise Integration Patterns (EIP)](https://www.enterpriseintegrationpatterns.com/patterns/messaging/) JSON
model. It is mainly used to
integrate Spring's library of integration components into
the [Keip Canvas UI](https://github.com/codice/keip-canvas).

## Getting Started

To build the shaded JAR, you will need the following installed:

- Java 21
- Maven 3.9

Clone the project and then build:

```shell
cd xsd-parser
mvn package
```

The JAR can then be executed with:

```shell
java -jar si-xsd-parser-cli/target/si-xsd-parser-cli.jar -h
```

```shell
Usage: <main class> [-hV] -o=<output> -s=<source>
Fetches and parses the Spring Integration XML schema files listed in the source
configuration file, then translates them into an EIP Schema.
  -h, --help              Show this help message and exit.
  -o, --output=<output>   Specify the path to write the translated EIP Schema
                            JSON. If a file does not exist at the path, it will
                            be created. If a file already exists, it will be
                            overwritten.
  -s, --source=<source>   Path to XML schema source file
  -V, --version           Print version information and exit.
```

## Configuration

The tool's main configuration settings are provided in a configuration YAML file. The following example illustrates the
available options:

```yaml
# A list of XSD sources
schemas:
  # Alias ends up as a top-level key in the translated EIP JSON
  - alias: "integration"
    # Target namespace 
    namespace: "http://www.springframework.org/schema/integration"
    # URI pointing to an XSD resource. Supported schemes: http/s, file.
    location: "https://www.springframework.org/schema/integration/spring-integration-5.2.xsd"
    # Elements listed here will be excluded from the output
    excludedElements:
      - "selector-chain"
      - "spel-property-accessors"
      - "converter"
      - "chain"

# The schemaLocations provided here will override any <xsd:import> elements referencing the listed namespaces
importedSchemaLocations:
  - namespace: "http://www.springframework.org/schema/beans"
    location: "https://www.springframework.org/schema/beans/spring-beans-4.3.xsd"
```

## How It Works

The parser relies on the [Apache XML Schema](https://ws.apache.org/xmlschema/) library to parse XSDs into a Java
object tree, which is then translated to the EIP Schema output model.

An example (abbreviated) output JSON:

```json
{
  "integration": [
    {
      "role": "endpoint",
      "connectionType": "passthru",
      "name": "filter",
      "description": "Defines a Consumer Endpoint for the 'org.springframework.integration.filter.MessageFilter' that is used to decide whether a Message should be passed along or dropped based on some criteria",
      "attributes": [
        {
          "name": "input-channel",
          "type": "string",
          "description": "The receiving Message channel of this endpoint",
          "required": false
        }
      ],
      "childGroup": {
        "indicator": "sequence",
        "children": [
          {
            "name": "expression",
            "attributes": [
              {
                "name": "key",
                "type": "string",
                "description": "The key for retrieving the expression from an ExpressionSource.",
                "required": true
              }
            ]
          }
        ]
      }
    }
  ]
}
```

The end result is a catalog of EIP component definitions with their assorted attributes and children.

XSDs tend to
grow quite complex, leading to deep levels of child nesting, some simplifications are made to mitigate this:

- Redundant XSD order indicators are collapsed where possible.
- A max cutoff level for child nesting is applied (defaults to 3). Since deeper levels of nesting are rarely useful for
  the intended use-cases of the EIP schema.

### Caveats

Instead of supporting references to base elements as in XSDs (e.g. base complex types and refs), child elements are
inlined in the output JSON. This keeps the code simpler, but has some drawbacks:

- The EIP JSON files can be large due to the duplication of child elements.
- Circular XSD element references are not fully supported.

## Package Generated Schema Definitions

To automatically run the parser cli tool and package its output as a JAR,
see the [eip-schema-definitions](./eip-schema-definitions/README.md) module