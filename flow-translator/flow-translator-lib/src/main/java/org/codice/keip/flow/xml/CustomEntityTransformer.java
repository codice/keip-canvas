package org.codice.keip.flow.xml;

import com.ctc.wstx.stax.WstxEventFactory;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.TransformerException;
import org.codice.keip.flow.error.TransformationError;

/** Validates Custom Entity XML content and adds the required id attribute before writing content */
final class CustomEntityTransformer {

  // These events will not be processed and are excluded from output.
  private static final Set<Integer> IGNORED_EVENTS =
      Set.of(XMLEvent.START_DOCUMENT, XMLEvent.END_DOCUMENT);

  private final XMLEventFactory eventFactory = WstxEventFactory.newFactory();
  private final XMLInputFactory inputFactory;

  CustomEntityTransformer(XMLInputFactory inputFactory) {
    this.inputFactory = inputFactory;
  }

  /**
   * Validates that the XML content for all custom entities is well-formed and an 'id' attribute to
   * the content's root element matching its key in the customEntities Map.
   *
   * @param customEntities user-defined map of entityId to content
   * @param writer where the transformed content XML will be written to
   * @return An empty list for a successful transformation, otherwise a non-empty list of {@link
   *     TransformationError} is returned.
   */
  List<TransformationError> apply(Map<String, String> customEntities, XMLEventWriter writer) {
    List<TransformationError> errors = new ArrayList<>();

    customEntities.forEach(
        (id, xml) -> {
          try {
            Deque<XMLEvent> eventQueue = new ArrayDeque<>();
            boolean wasRootIdAttributeAdded = false;

            XMLEventReader reader = inputFactory.createXMLEventReader(new StringReader(xml));
            while (reader.hasNext()) {
              XMLEvent event = reader.nextEvent();

              if (IGNORED_EVENTS.contains(event.getEventType())) {
                continue;
              }

              if (!wasRootIdAttributeAdded && event.isStartElement()) {
                event = addIdAttributeToEntityRoot(event.asStartElement(), id);
                wasRootIdAttributeAdded = true;
              }

              eventQueue.addLast(event);
            }

            // only writes events once all content for this entity is processed without exceptions
            for (XMLEvent e : eventQueue) {
              writer.add(e);
            }

          } catch (XMLStreamException e) {
            errors.add(
                new TransformationError(
                    String.format("custom entity [%s]", id),
                    new TransformerException(e.getMessage())));
          }
        });

    return errors;
  }

  private XMLEvent addIdAttributeToEntityRoot(StartElement root, String entityId) {
    List<Attribute> updatedAttrs = new ArrayList<>();
    updatedAttrs.add(eventFactory.createAttribute("id", entityId));

    Iterator<Attribute> attrs = root.getAttributes();
    while (attrs.hasNext()) {
      var attr = attrs.next();
      if (!"id".equals(attr.getName().getLocalPart())) {
        updatedAttrs.add(attr);
      }
    }
    return eventFactory.createStartElement(
        root.getName(), updatedAttrs.iterator(), root.getNamespaces());
  }
}
