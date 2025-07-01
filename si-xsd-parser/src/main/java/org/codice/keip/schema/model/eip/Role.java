package org.codice.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

public enum Role {
  @SerializedName("channel")
  CHANNEL,
  @SerializedName("endpoint")
  ENDPOINT,
  @SerializedName("router")
  ROUTER,
  @SerializedName("transformer")
  TRANSFORMER,
}
