# Generated EIP Schema Definitions

This module runs the `si-xsd-parser-cli` tool and packages the output EIP component definition JSON as a JAR.

## Configuration

The module uses the [xsd-sources.yaml](xsd-sources.yaml) file to configure the xsd parser.

## Building

```shell
mvn package -P runParser
```

This will execute the xsd parser and build a JAR with the EIP definitions JSON at the
root.

Note: the build will fail if the parser returns a non-zero exit code (e.g. due to a misconfigured `xsd-sources.yaml`)

## Usage

Add the dependency:

```xml

<dependency>
    <groupId>org.codice.keip.xsd</groupId>
    <artifactId>eip-schema-definitions</artifactId>
    <version>${version}</version>
</dependency>
```

Read component definitions JSON from the classpath:

```java
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Consumer {
    public static void main(String[] args) throws Exception {
        try (InputStream in = Consumer.class.getResourceAsStream("/springIntegrationEipComponents.json")) {
            if (in == null) {
                throw new IllegalStateException("not found on classpath");
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(content);
        }
    }
}

```