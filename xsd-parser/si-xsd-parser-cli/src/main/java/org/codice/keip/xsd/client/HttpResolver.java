package org.codice.keip.xsd.client;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResolver implements UriResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpResolver.class);

  private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(20);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

  private final HttpClient httpClient;

  public HttpResolver() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).build();
  }

  HttpResolver(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public InputStream fetchUri(URI uri) throws Exception {
    HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(REQUEST_TIMEOUT).GET().build();
    HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());

    if (response.statusCode() == 200) {
      return response.body();
    }

    LOGGER.error("Response code: {}", response.statusCode());
    throw new RuntimeException("Failed to retrieve the target schema");
  }
}
