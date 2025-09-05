package org.codice.keip.xsd.client

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.io.FileSystemFixture

import java.nio.file.Path

class FileResolverTest extends Specification {

    def fileResolver = new FileResolver()

    @TempDir
    @Shared
    FileSystemFixture fsFixture

    def setupSpec() {
        fsFixture.create {
            dir("testdir") {
                file("testfile") << "foobar"
            }
        }
    }

    def "Fetch local file from absolute URI success"() {
        given:
        def uri = fsFixture.resolve("testdir/testfile").toUri()

        when:
        def fileStream = fileResolver.fetchUri(uri)

        then:
        fileStream.text == "foobar"
    }

    def "Fetch local file from 'relative' URI success"() {
        given:
        def relative = Path.of("src", "test", "resources", "test-relative-path.txt")
        def uri = URI.create("file:" + relative.toString())

        when:
        def fileStream = fileResolver.fetchUri(uri)

        then:
        fileStream.text == "foobar"
    }

    def "Fetch non-existent file throws exception"() {
        given:
        def uri = fsFixture.resolve(Path.of("fake", "testdirpath")).toUri()

        when:
        def fileStream = fileResolver.fetchUri(uri)

        then:
        thrown(IllegalArgumentException)
    }
}
