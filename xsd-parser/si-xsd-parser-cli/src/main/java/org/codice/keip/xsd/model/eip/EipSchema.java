package org.codice.keip.xsd.model.eip;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// TODO: Rename to match EipComponentDefinition schema
// TODO: Look into generating this class from the JSON schema.
// Should match schema at:
// <project-root>/keip-canvas/schemas/model/json/eipComponentDef.schema.json
public class EipSchema {

  private final Map<String, List<EipComponent>> eipComponentMap;

  public EipSchema() {
    this.eipComponentMap = new LinkedHashMap<>();
  }

  private EipSchema(Map<String, List<EipComponent>> eipComponentMap) {
    this.eipComponentMap = eipComponentMap;
  }

  public static EipSchema from(Map<String, List<EipComponent>> componentMap) {
    return new EipSchema(componentMap);
  }

  public Map<String, List<EipComponent>> toMap() {
    return eipComponentMap;
  }

  public void addComponents(String namespace, List<EipComponent> components) {
    eipComponentMap.put(namespace, components);
  }
}
