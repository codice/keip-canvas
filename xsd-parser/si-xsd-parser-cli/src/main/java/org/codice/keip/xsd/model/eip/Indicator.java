package org.codice.keip.xsd.model.eip;

import com.google.gson.annotations.SerializedName;

public enum Indicator {
  @SerializedName("all")
  ALL,
  @SerializedName("choice")
  CHOICE,
  @SerializedName("sequence")
  SEQUENCE;
}
