package org.codice.keip.flow;

import org.codice.keip.flow.model.ConnectionType;
import org.codice.keip.flow.model.EipId;
import org.codice.keip.flow.model.Role;

// TODO: Javadoc
public interface ComponentRegistry {

  boolean isRegistered(EipId id);

  ConnectionType getConnectionType(EipId id);

  Role getRole(EipId id);
}
