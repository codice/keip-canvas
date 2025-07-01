package org.codice.keip.flow.xml;

import org.xmlunit.builder.DiffBuilder

import java.nio.file.Path;

class XmlComparisonUtil {

    static void compareXml(Object actual, Object expected) {
        def diff = DiffBuilder.compare(expected)
                              .withTest(actual)
                              .checkForIdentical()
                              .ignoreWhitespace()
                              .normalizeWhitespace()
                              .build()

        assert !diff.hasDifferences()
    }

    static InputStream readTestXml(String filename) {
        Path path = Path.of("xml").resolve(filename)
        return XmlComparisonUtil.class.getClassLoader()
                                .getResourceAsStream(path.toString())
    }
}
