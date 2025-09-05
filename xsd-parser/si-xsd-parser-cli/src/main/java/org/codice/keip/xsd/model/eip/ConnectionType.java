package org.codice.keip.xsd.model.eip;

import com.google.gson.annotations.SerializedName;

public enum ConnectionType {
  @SerializedName("content_based_router")
  CONTENT_BASED_ROUTER,
  @SerializedName("inbound_request_reply")
  INBOUND_REQUEST_REPLY,
  @SerializedName("passthru")
  PASSTHRU,
  @SerializedName("request_reply")
  REQUEST_REPLY,
  @SerializedName("sink")
  SINK,
  @SerializedName("source")
  SOURCE,
  @SerializedName("tee")
  TEE,
}
