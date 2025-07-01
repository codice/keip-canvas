package org.codice.keip.flow.web.translation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.codice.keip.flow.model.Flow;
import io.swagger.v3.oas.annotations.Operation;
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

  @Operation(summary = "Translate an EIP flow json to a Spring Integration XML")
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  ResponseEntity<TranslationResponse> flowToXml(
      @RequestBody Flow eipFlow, @RequestParam(defaultValue = "false") boolean prettyPrint) {
    TranslationResponse response = this.flowTranslationService.toXml(eipFlow, prettyPrint);
    if (response.error() == null) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }
}
