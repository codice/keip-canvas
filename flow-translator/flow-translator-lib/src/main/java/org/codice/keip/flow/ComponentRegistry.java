package org.codice.keip.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.codice.keip.flow.model.ConnectionType;
import org.codice.keip.flow.model.EipId;
import org.codice.keip.flow.model.Role;

/** A registry for looking up registered EIP components along with their associated properties. */
public final class ComponentRegistry {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Map<EipId, ComponentProperties> registryMap;

  private ComponentRegistry(Map<EipId, ComponentProperties> registryMap) {
    this.registryMap = registryMap;
  }

  public boolean isRegistered(EipId id) {
    return registryMap.containsKey(id);
  }

  public Optional<ConnectionType> getConnectionType(EipId id) {
    return Optional.ofNullable(registryMap.get(id)).map(ComponentProperties::connectionType);
  }

  public Optional<Role> getRole(EipId id) {
    return Optional.ofNullable(registryMap.get(id)).map(ComponentProperties::role);
  }

  /**
   * Parses an EIP component definition JSON. The provided JSON must match the schema defined <a
   * href="https://github.com/codice/keip-canvas/blob/main/schemas/model/json/eipComponentDef.schema.json">here</a>.
   */
  public static ComponentRegistry fromJson(InputStream json) throws IOException {
    Map<EipId, ComponentProperties> regMap = new HashMap<>();

    Map<String, JsonNode> jsonMap = MAPPER.readValue(json, new TypeReference<>() {});

    for (var namespace : jsonMap.entrySet()) {
      for (var component : namespace.getValue()) {
        String name = component.get("name").textValue();
        ConnectionType connectionType =
            ConnectionType.valueOf(component.get("connectionType").textValue().toUpperCase());
        Role role = Role.valueOf(component.get("role").textValue().toUpperCase());
        regMap.put(
            new EipId(namespace.getKey(), name), new ComponentProperties(connectionType, role));
      }
    }

    return new ComponentRegistry(regMap);
  }

  private record ComponentProperties(ConnectionType connectionType, Role role) {}
}
