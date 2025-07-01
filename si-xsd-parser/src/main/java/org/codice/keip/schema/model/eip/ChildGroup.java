package org.codice.keip.schema.model.eip;

import java.util.ArrayList;
import java.util.List;

public record ChildGroup(Indicator indicator, Occurrence occurrence, List<ChildComposite> children)
    implements ChildComposite {
  public ChildGroup(Indicator indicator, Occurrence occurrence) {
    this(indicator, occurrence, new ArrayList<>());
  }

  public ChildGroup(Indicator indicator, List<ChildComposite> children) {
    this(indicator, null, children);
  }

  @Override
  public void addChild(ChildComposite child) {
    children.add(child);
  }

  @Override
  public Occurrence occurrence() {
    return this.occurrence == null ? Occurrence.DEFAULT : this.occurrence;
  }

  @Override
  public ChildComposite withOccurrence(Occurrence occurrence) {
    return new ChildGroup(this.indicator, occurrence, this.children);
  }

  public ChildGroup withChildren(List<ChildComposite> children) {
    return new ChildGroup(this.indicator, this.occurrence, children);
  }
}
