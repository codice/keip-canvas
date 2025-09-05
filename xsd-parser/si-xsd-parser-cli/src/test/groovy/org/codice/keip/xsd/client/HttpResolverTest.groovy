package org.codice.keip.xsd.client

import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.nio.file.Path

class HttpResolverTest extends Specification {

    HttpClient httpClient = Mock(HttpClient)

    def httpResolver = new HttpResolver(httpClient)

    def "Fetch URI success returns InputStream"(String uri) {
        given:
        def expectedStream = getXmlInputStream("first.xsd")
        httpClient.send(_, _) >> mockHttpResponse(expectedStream)

        when:
        def response = httpResolver.fetchUri(URI.create(uri))

        then:
        response == expectedStream

        where:
        uri << ["http://localhost/first", "https://localhost/first"]
    }

    def "Fetch URI error response code throws exception"() {
        given:
        def errorResponse = Mock(HttpResponse)
        errorResponse.statusCode() >> 404
        httpClient.send(_, _) >> errorResponse

        when:
        httpResolver.fetchUri(URI.create("http://localhost/first"))

        then:
        thrown(RuntimeException)
    }

    def "HttpClient request throws an unhandled exception"() {
        given:
        httpClient.send(_, _) >> { throw new ConnectException("server down") }

        when:
        httpResolver.fetchUri(URI.create("http://localhost/first"))

        then:
        thrown(ConnectException)
    }

    private HttpResponse<InputStream> mockHttpResponse(InputStream is) {
        HttpResponse<InputStream> schemaResponse = Mock(HttpResponse)
        schemaResponse.body() >> is
        schemaResponse.statusCode() >> 200
        return schemaResponse
    }

    private static InputStream getXmlInputStream(String filename) {
        String path = Path.of("schemas", "xml", "client", filename).toString()
        return XmlSchemaHttpClientTest.class.getClassLoader().getResource(path).newInputStream()
    }
}
