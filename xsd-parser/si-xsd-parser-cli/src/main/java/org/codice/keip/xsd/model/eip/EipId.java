package org.codice.keip.xsd.model.eip;

public record EipId(String namespace, String name) {
  @Override
  public String toString() {
    return namespace + ":" + name;
  }
}
