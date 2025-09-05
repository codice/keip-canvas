package org.codice.keip.xsd.test

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.codice.keip.xsd.model.eip.ChildComposite
import org.codice.keip.xsd.model.eip.Occurrence
import org.codice.keip.xsd.model.eip.Restriction
import org.codice.keip.xsd.serdes.ChildCompositeDeserializer
import org.codice.keip.xsd.serdes.OccurrenceDeserializer
import org.codice.keip.xsd.serdes.RestrictionDeserializer

import java.nio.file.Path

class TestIOUtils {

    static BufferedReader getXmlSchemaFileReader(Path filepath) {
        Path path = Path.of("schemas", "xml").resolve(filepath)
        return TestIOUtils.class.getClassLoader().getResource(path.toString()).newReader()
    }

    static BufferedInputStream getYamlConfig(Path filePath) {
        String path = Path.of("source-configs").resolve(filePath)
        return TestIOUtils.class.getClassLoader().getResource(path.toString()).newInputStream()
    }

    static Gson configureGson() {
        var gson = new GsonBuilder()
        gson.registerTypeAdapter(Restriction.class, new RestrictionDeserializer())
        gson.registerTypeAdapter(ChildComposite.class, new ChildCompositeDeserializer())
        gson.registerTypeAdapter(Occurrence.class, new OccurrenceDeserializer())
        return gson.create()
    }
}
