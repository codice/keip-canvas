package org.codice.keip.schema.xml;

import org.codice.keip.schema.model.eip.ChildGroup;
import org.codice.keip.schema.model.eip.EipChildElement;
import org.codice.keip.schema.model.eip.EipComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.walker.XmlSchemaWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Translates a Spring Integration XML schema into a list of {@link EipComponent} */
public class SchemaTranslator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaTranslator.class);

  private final Set<String> excludedComponentNames;

  private ChildGroupReducer groupReducer = new ChildGroupReducer();

  public SchemaTranslator(Set<String> excludedComponentNames) {
    this.excludedComponentNames = excludedComponentNames;
  }

  void setGroupReducer(ChildGroupReducer groupReducer) {
    this.groupReducer = groupReducer;
  }

  public List<EipComponent> translate(XmlSchemaCollection schemaCol, XmlSchema targetSchema) {
    var eipComponents = new ArrayList<EipComponent>();
    var eipVisitor = new EipTranslationVisitor();
    var schemaWalker = new XmlSchemaWalker(schemaCol);
    schemaWalker.addVisitor(eipVisitor);

    for (XmlSchemaElement element : targetSchema.getElements().values()) {
      if (excludedComponentNames.contains(element.getName())) {
        LOGGER.debug("Skipping component: {}", element.getName());
        continue;
      }

      LOGGER.debug("Translating component: {}", element.getName());

      try {
        eipVisitor.reset();
        schemaWalker.walk(element);

        EipComponent eipComponent = eipVisitor.getEipComponent();
        ChildGroup reduced = groupReducer.reduceGroup(eipComponent.getChildGroup());
        eipComponent.setChildGroup(reduced);
        eipComponents.add(eipComponent);
      } catch (Exception e) {
        LOGGER.error("Error translating component: {}", element.getName(), e);
      }
    }

    checkTopLevelFreeOfNestedGroups(eipComponents);
    return eipComponents;
  }

  private void checkTopLevelFreeOfNestedGroups(List<EipComponent> components) {
    for (var component : components) {
      if (component.getChildGroup() != null) {
        for (var child : component.getChildGroup().children()) {
          if (!(child instanceof EipChildElement)) {
            throw new IllegalArgumentException(
                String.format(
                    "Top level child group could not be reduced for component: %s",
                    component.getName()));
          }
        }
      }
    }
  }
}
