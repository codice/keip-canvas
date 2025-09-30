package org.codice.keip.xsd.model.eip;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class EipElement {

  protected final EipId eipId;
  protected final String description;
  protected Set<Attribute> attributes;
  protected ChildComposite childGroup;

  protected EipElement(Builder<?> builder) {
    this.eipId = builder.eipId;
    this.description = builder.description;
    this.attributes = builder.attributes;
    this.childGroup = builder.childGroup;
  }

  public EipId getEipId() {
    return eipId;
  }

  public String getDescription() {
    return description;
  }

  public Set<Attribute> getAttributes() {
    if (attributes == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(attributes);
  }

  public ChildComposite getChildGroup() {
    return childGroup;
  }

  public void setChildGroup(ChildComposite childGroup) {
    this.childGroup = childGroup;
  }

  public void addAttribute(Attribute attribute) {
    if (this.attributes == null) {
      this.attributes = new HashSet<>();
    }
    this.attributes.add(attribute);
  }

  @Override
  public String toString() {
    return this.eipId.toString();
  }

  // Effective Java - Hierarchical builder pattern
  protected abstract static class Builder<T extends Builder<T>> {

    protected EipId eipId;
    protected String description;
    protected Set<Attribute> attributes;
    protected ChildComposite childGroup;

    public Builder() {}

    public Builder(EipElement element) {
      this.eipId = element.eipId;
      this.description = element.description;
      this.attributes = element.attributes;
      this.childGroup = element.childGroup;
    }

    public T eipId(EipId eipId) {
      this.eipId = eipId;
      return self();
    }

    public T description(String description) {
      this.description = description;
      return self();
    }

    public T attributes(Set<Attribute> attributes) {
      this.attributes = attributes;
      return self();
    }

    public T addAttribute(Attribute attribute) {
      if (this.attributes == null) {
        this.attributes = new HashSet<>();
      }
      this.attributes.add(attribute);
      return self();
    }

    public T childGroup(ChildComposite childGroup) {
      this.childGroup = childGroup;
      return self();
    }

    protected abstract EipElement build();

    protected abstract T self();
  }
}
