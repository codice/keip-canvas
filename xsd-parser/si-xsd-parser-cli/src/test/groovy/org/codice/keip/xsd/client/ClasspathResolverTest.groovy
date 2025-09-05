package org.codice.keip.xsd.client


import spock.lang.Specification

class ClasspathResolverTest extends Specification {

    def classpathResolver = new ClasspathResolver()

    def "Fetch classpath file success"() {
        given:
        def uri = new URI("classpath:test-relative-path.txt")

        when:
        def istream = classpathResolver.fetchUri(uri)

        then:
        istream.text == "foobar"
    }

    def "Fetch non-existent file throws exception"() {
        given:
        def uri = new URI("classpath:/fake/none.txt")

        when:
        classpathResolver.fetchUri(uri)

        then:
        thrown(IllegalArgumentException)
    }
}
