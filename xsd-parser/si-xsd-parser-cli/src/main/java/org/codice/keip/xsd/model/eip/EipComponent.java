package org.codice.keip.xsd.model.eip;

import java.util.Objects;

/** A top-level EIP component: Message Endpoint or Channel. */
public final class EipComponent extends EipElement {

  private final Role role;

  private final ConnectionType connectionType;

  private EipComponent(Builder builder) {
    super(builder);
    this.role = builder.role;
    this.connectionType = builder.connectionType;
  }

  public Role getRole() {
    return role;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public static class Builder extends EipElement.Builder<Builder> {

    private Role role;
    private ConnectionType connectionType;

    public Builder(String name, Role role, ConnectionType connectionType) {
      this.name = Objects.requireNonNull(name);
      this.role = Objects.requireNonNull(role);
      this.connectionType = Objects.requireNonNull(connectionType);
    }

    public Builder role(Role role) {
      this.role = role;
      return self();
    }

    public Builder connectionType(ConnectionType connectionType) {
      this.connectionType = connectionType;
      return self();
    }

    @Override
    public EipComponent build() {
      return new EipComponent(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
