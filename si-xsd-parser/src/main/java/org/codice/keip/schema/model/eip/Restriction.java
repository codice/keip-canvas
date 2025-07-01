package org.codice.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Note: This is not an exhaustive Restriction set. XML schemas support many more restriction types.
// TODO: Decide which ones should be supported.
public sealed interface Restriction {

  enum RestrictionType {
    @SerializedName("enum")
    ENUM;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  RestrictionType type();

  record MultiValuedRestriction(RestrictionType type, List<String> values) implements Restriction {}
  ;
}
