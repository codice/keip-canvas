package org.codice.keip.flow.web.translation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import io.swagger.v3.oas.annotations.Operation;
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
@RestController
@RequestMapping("/")
class TranslationController {

  private final TranslationService flowTranslationService;

  TranslationController(TranslationService flowTranslationService) {
    this.flowTranslationService = flowTranslationService;
  }

  @Operation(summary = "Translate an EIP Flow json to a Spring Integration XML")
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  ResponseEntity<TranslationResponse<String>> flowToXml(
      @RequestBody Flow eipFlow, @RequestParam(defaultValue = "false") boolean prettyPrint) {
    TranslationResponse<String> response = this.flowTranslationService.toXml(eipFlow, prettyPrint);
    if (response.error() == null) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  @Operation(summary = "Translate a Spring Integration XML to an EIP Flow json")
  @PostMapping(consumes = APPLICATION_XML_VALUE, produces = APPLICATION_JSON_VALUE)
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
