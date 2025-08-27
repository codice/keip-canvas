package org.codice.keip.flow;

import org.codice.keip.flow.model.ConnectionType;
import org.codice.keip.flow.model.EipId;
import org.codice.keip.flow.model.Role;

public interface ComponentRegistry {
  ConnectionType getConnectionType(EipId id);

  Role getRole(EipId id);
}
