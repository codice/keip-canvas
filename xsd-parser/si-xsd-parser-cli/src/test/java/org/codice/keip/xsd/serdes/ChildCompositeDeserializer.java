package org.codice.keip.xsd.serdes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import org.codice.keip.xsd.model.eip.ChildComposite;
import org.codice.keip.xsd.model.eip.ChildGroup;
import org.codice.keip.xsd.model.eip.EipChildElement;

public class ChildCompositeDeserializer implements JsonDeserializer<ChildComposite> {
  @Override
  public ChildComposite deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    if (json.getAsJsonObject().has("indicator")) {
      return context.deserialize(json, ChildGroup.class);
    }
    return context.deserialize(json, EipChildElement.class);
  }
}
