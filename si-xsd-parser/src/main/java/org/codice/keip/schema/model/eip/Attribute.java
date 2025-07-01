package org.codice.keip.schema.model.eip;

import com.google.gson.annotations.SerializedName;

public record Attribute(
    String name,
    AttributeType type,
    String description,
    @SerializedName("default") String defaultValue,
    boolean required,
    Restriction restriction) {

  public Attribute(Builder builder) {
    this(
        builder.name,
        builder.type,
        builder.description,
        builder.defaultValue,
        builder.required,
        builder.restriction);
  }

  public static class Builder {
    private final String name;
    private final AttributeType type;
    private String description;
    private String defaultValue;
    private boolean required;
    private Restriction restriction;

    public Builder(String name, AttributeType type) {
      this.name = name;
      this.type = type;
    }

    public Attribute build() {
      return new Attribute(this);
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder defaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder required(boolean required) {
      this.required = required;
      return this;
    }

    public Builder restriction(Restriction restriction) {
      this.restriction = restriction;
      return this;
    }
  }
}
