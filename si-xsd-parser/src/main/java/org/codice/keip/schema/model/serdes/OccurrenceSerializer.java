package org.codice.keip.schema.model.serdes;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.codice.keip.schema.model.eip.Occurrence;
import java.lang.reflect.Type;

public class OccurrenceSerializer implements JsonSerializer<Occurrence> {
  @Override
  public JsonElement serialize(Occurrence occurrence, Type type, JsonSerializationContext context) {
    if (occurrence.isDefault()) {
      return null;
    }
    return context.serialize(occurrence.toMinimalMap());
  }
}
