package org.codice.keip.xsd.model.eip;

import java.util.List;
import java.util.Objects;

public final class EipChildElement extends EipElement implements ChildComposite {

  private Occurrence occurrence;

  private EipChildElement(Builder builder) {
    super(builder);
    this.occurrence = builder.occurrence;
  }

  @Override
  public void addChild(ChildComposite child) {
    this.setChildGroup(child);
  }

  @Override
  public List<ChildComposite> children() {
    return this.getChildGroup() == null ? null : this.getChildGroup().children();
  }

  @Override
  public Occurrence occurrence() {
    return this.occurrence == null ? Occurrence.DEFAULT : this.occurrence;
  }

  @Override
  public ChildComposite withOccurrence(Occurrence occurrence) {
    this.occurrence = occurrence;
    return this;
  }

  public EipChildElement withChildGroup(ChildComposite child) {
    return new Builder(this).childGroup(child).build();
  }

  public static class Builder extends EipElement.Builder<Builder> {

    private Occurrence occurrence;

    public Builder(String name) {
      this.name = Objects.requireNonNull(name);
    }

    public Builder(EipChildElement element) {
      super(element);
      this.occurrence = element.occurrence;
    }

    public Builder occurrence(Occurrence occurrence) {
      this.occurrence = occurrence;
      return self();
    }

    @Override
    public EipChildElement build() {
      return new EipChildElement(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
