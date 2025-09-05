package org.codice.keip.xsd.model.serdes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.codice.keip.xsd.model.eip.EipSchema;
import org.codice.keip.xsd.model.eip.Occurrence;

public class SchemaSerializer {
  static final Gson GSON =
      new GsonBuilder().registerTypeAdapter(Occurrence.class, new OccurrenceSerializer()).create();

  public static void writeSchemaToJsonFile(EipSchema eipSchema, File file) throws IOException {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }

    try (Writer writer = new FileWriter(file)) {
      GSON.toJson(eipSchema.toMap(), writer);
    }
  }
}
