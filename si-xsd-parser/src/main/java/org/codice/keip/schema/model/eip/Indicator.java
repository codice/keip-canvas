package org.codice.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

public enum Indicator {
  @SerializedName("all")
  ALL,
  @SerializedName("choice")
  CHOICE,
  @SerializedName("sequence")
  SEQUENCE;
}
