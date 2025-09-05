package org.codice.keip.xsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.keip.xsd.client.XmlSchemaClient;
import org.codice.keip.xsd.config.XsdSourceConfiguration;
import org.codice.keip.xsd.config.XsdSourceConfiguration.SchemaIdentifier;
import org.codice.keip.xsd.model.eip.EipComponent;
import org.codice.keip.xsd.model.eip.EipSchema;
import org.codice.keip.xsd.xml.SchemaTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EipSchemaTranslation {

  private static final Logger LOGGER = LoggerFactory.getLogger(EipSchemaTranslation.class);

  private final XmlSchemaClient xmlSchemaClient;

  private final EipSchema eipSchema;

  private final List<TransformerException> errors;

  public EipSchemaTranslation(
      XsdSourceConfiguration sourceConfiguration, XmlSchemaClient xmlSchemaClient) {
    this.errors = new ArrayList<>();
    this.xmlSchemaClient = xmlSchemaClient;
    this.eipSchema = translate(sourceConfiguration);
  }

  public EipSchema getEipSchema() {
    return eipSchema;
  }

  public List<TransformerException> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  private EipSchema translate(XsdSourceConfiguration sourceConfiguration) {
    EipSchema translatedSchema = new EipSchema();

    for (SchemaIdentifier targetSchema : sourceConfiguration.getSchemas()) {
      try {
        XmlSchemaCollection schemaCollection = xmlSchemaClient.collect(targetSchema.getLocation());

        var translator = new SchemaTranslator(targetSchema.getExcludedElements());

        List<EipComponent> components =
            translator.translate(
                schemaCollection, schemaCollection.schemaForNamespace(targetSchema.getNamespace()));

        translatedSchema.addComponents(targetSchema.getAlias(), components);
      } catch (Exception e) {
        String message =
            String.format("Failed to translate schema: %s", targetSchema.getNamespace());
        LOGGER.error(message, e);
        errors.add(new TransformerException(message, e));
      }
    }

    return translatedSchema;
  }
}
