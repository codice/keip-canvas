# EIP Flow Translator Webapp

A web application providing access to the [Flow Translator Library](/flow-translator/flow-translator-lib/README.md) over
HTTP endpoints. Built with Spring Boot.

## Quick Start

Build the application JAR and Docker image (Requires Java 21 and Maven 3.9+):

```shell
mvn package
```

Run the JAR:

```shell
java -jar ./target/flow-translator-webapp-0.1.0.jar
```

Or in a Docker container:

```shell
docker run --name flow-translator -p 8080:8080 ghcr.io/codice/keip-canvas/flow-translator-webapp
```

## API Documentation

You can browse the endpoint documentation by running the app and navigating to http://localhost:8080/doc in a browser.

## CORS configuration

To allow web browser requests from different domains to interact with the translator app, CORS headers must be included
in the responses. Use the following environment variables to enable and configure CORS:

- `WEB_CORS_ENABLED`: set to `true` to enable CORS headers.
    - Note if this variable is not set to `true`, the other CORS configuration values will have no effect.
- `WEB_CORS_ALLOWED_ORIGINS`: sets the `Access-Control-Allow-Origin` header to the provided value.
    - To specify multiple domains, use a comma-separated list (e.g. `https://local.domain,http://localhost:8123`).
    - WARNING: If CORS is enabled and this variable isn't set, all origins are
      allowed (`Access-Control-Allow-Origin = '*'`)
- `WEB_CORS_ALLOWED_METHODS`: sets the `Access-Control-Allow-Methods` header to the provided value.
    - To specify multiple HTTP methods, use a comma-separated list (e.g. `GET,POST`).
    - WARNING: If CORS is enabled and this variable isn't set, all HTTP methods accepted by the endpoint's controller
      mapping are allowed.

To access Spring Boot Actuator endpoints (such as the healthcheck `/actuator/health`), CORS must be enabled for
those endpoints separately:

- `MANAGEMENT_ENDPOINTS_WEB_CORS_ALLOWED_ORIGINS`: sets the `Access-Control-Allow-Origin` header.
- `MANAGEMENT_ENDPOINTS_WEB_CORS_ALLOWED_HEADERS`: sets the `Access-Control-Allow-Headers` header.

Note: As an alternative to environment
variables, [Spring Boot properties](https://docs.spring.io/spring-boot/reference/features/external-config.html) can also
be used to configure CORS.