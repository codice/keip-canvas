package org.codice.keip.flow.web.translation;

import org.codice.keip.flow.FlowTranslator;
import org.codice.keip.flow.error.TransformationError;
import org.codice.keip.flow.model.Flow;
import org.codice.keip.flow.web.error.ApiError;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class TranslationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TranslationService.class);

  private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

  private final FlowTranslator flowTranslator;

  TranslationService(FlowTranslator flowTranslator) {
    this.flowTranslator = flowTranslator;
  }

  TranslationResponse toXml(Flow flow) {
    return toXml(flow, false);
  }

  TranslationResponse toXml(Flow flow, boolean prettyPrint) {
    try (StringWriter output = new StringWriter()) {
      List<TransformationError> errors = this.flowTranslator.toXml(flow, output);
      String data =
          prettyPrint ? this.prettyPrint(new StringReader(output.toString())) : output.toString();
      return new TranslationResponse(data, toApiError(errors));
    } catch (TransformerException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String prettyPrint(Reader rawXml) throws TransformerException {
    Transformer transformer = this.transformerFactory.newTransformer();

    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    // Add a line break after XML declaration
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");

    StringWriter output = new StringWriter();
    transformer.transform(new StreamSource(rawXml), new StreamResult(output));
    return output.toString();
  }

  private ApiError<TranslationErrorDetail> toApiError(List<TransformationError> errors) {
    if (errors.isEmpty()) {
      return null;
    }

    LOGGER.atDebug().setMessage("Partial translation errors: {}").addArgument(errors).log();

    List<TranslationErrorDetail> errorDetails =
        errors.stream().map(TranslationErrorDetail::from).toList();
    return new ApiError<>(
        "Failed to transform one or more nodes", "PARTIAL_TRANSFORM", errorDetails);
  }
}
