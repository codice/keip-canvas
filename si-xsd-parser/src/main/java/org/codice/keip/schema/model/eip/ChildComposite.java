package org.codice.keip.schema.model.eip;

import java.util.List;

public sealed interface ChildComposite permits ChildGroup, EipChildElement {

  void addChild(ChildComposite child);

  List<ChildComposite> children();

  Occurrence occurrence();

  ChildComposite withOccurrence(Occurrence occurrence);
}
