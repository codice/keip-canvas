package org.codice.keip.xsd.model.serdes;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.codice.keip.xsd.model.eip.Occurrence;

public class OccurrenceSerializer implements JsonSerializer<Occurrence> {
  @Override
  public JsonElement serialize(Occurrence occurrence, Type type, JsonSerializationContext context) {
    if (occurrence.isDefault()) {
      return null;
    }
    return context.serialize(occurrence.toMinimalMap());
  }
}
