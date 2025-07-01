package org.codice.keip.schema.serdes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.codice.keip.schema.model.eip.Occurrence;
import java.lang.reflect.Type;

public class OccurrenceDeserializer implements JsonDeserializer<Occurrence> {

  @Override
  public Occurrence deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    var jsonObj = json.getAsJsonObject();
    long min = jsonObj.getAsJsonPrimitive("min").getAsLong();
    long max = jsonObj.getAsJsonPrimitive("max").getAsLong();
    max = max == -1 ? Occurrence.UNBOUNDED : max;
    return new Occurrence(min, max);
  }
}
