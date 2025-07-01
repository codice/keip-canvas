package org.codice.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

// TODO: See if annotations can be removed from enum.
public enum AttributeType {
  @SerializedName("string")
  STRING,
  @SerializedName("boolean")
  BOOLEAN,
  @SerializedName("number")
  NUMBER;
}
