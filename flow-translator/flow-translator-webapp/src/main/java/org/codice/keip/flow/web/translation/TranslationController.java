package org.codice.keip.flow.web.translation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import org.codice.keip.flow.model.Flow;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: Add json validation endpoint

// Split translation into separate paths to allow for better OpenAPI doc generation
@RestController
@RequestMapping
public class TranslationController {

  private static final String ROOT_PATH = "/translation";
  public static final String FLOW_TO_XML_ENDPOINT = ROOT_PATH + "/toSpringXml";
  public static final String XML_TO_FLOW_ENDPOINT = ROOT_PATH + "/toFlow";

  private final TranslationService flowTranslationService;

  TranslationController(TranslationService flowTranslationService) {
    this.flowTranslationService = flowTranslationService;
  }

  @Operation(summary = "Translate an EIP Flow JSON to a Spring Integration XML")
  @PostMapping(
      path = FLOW_TO_XML_ENDPOINT,
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  ResponseEntity<TranslationResponse<String>> flowToXml(
      @RequestBody Flow eipFlow, @RequestParam(defaultValue = "false") boolean prettyPrint) {
    TranslationResponse<String> response = this.flowTranslationService.toXml(eipFlow, prettyPrint);
    if (response.error() == null) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @Operation(
      summary = "Translate a Spring Integration XML to an EIP Flow JSON",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              description = "Spring Integration XML",
              content =
                  @Content(
                      mediaType = APPLICATION_XML_VALUE,
                      schema = @Schema(type = "string", format = "xml"))))
  @PostMapping(
      path = XML_TO_FLOW_ENDPOINT,
      consumes = APPLICATION_XML_VALUE,
      produces = APPLICATION_JSON_VALUE)
  ResponseEntity<TranslationResponse<Flow>> xmlToFlow(HttpServletRequest request)
      throws IOException {
    try (InputStream body = request.getInputStream()) {
      TranslationResponse<Flow> response = this.flowTranslationService.fromXml(body);
      if (response.error() == null) {
        return ResponseEntity.ok(response);
      } else {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
    }
  }
}
