package org.codice.keip.schema;

import org.codice.keip.schema.client.XmlSchemaClient;
import org.codice.keip.schema.config.XsdSourceConfiguration;
import org.codice.keip.schema.config.XsdSourceConfiguration.SchemaIdentifier;
import org.codice.keip.schema.model.eip.EipComponent;
import org.codice.keip.schema.model.eip.EipSchema;
import org.codice.keip.schema.xml.SchemaTranslator;
import java.util.List;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EipSchemaTranslation {

  private static final Logger LOGGER = LoggerFactory.getLogger(EipSchemaTranslation.class);

  private final XmlSchemaClient xmlSchemaClient;

  private final EipSchema eipSchema;

  public EipSchemaTranslation(
          XsdSourceConfiguration sourceConfiguration, XmlSchemaClient xmlSchemaClient) {
    this.xmlSchemaClient = xmlSchemaClient;
    this.eipSchema = translate(sourceConfiguration);
  }

  public EipSchema getEipSchema() {
    return eipSchema;
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
        LOGGER.error("Failed to translate schema: {}", targetSchema.getNamespace(), e);
      }
    }

    return translatedSchema;
  }
}
