package org.codice.keip.flow.web;

import static org.codice.keip.flow.web.FlowTranslatorAppTest.readFlowJson;
import static org.codice.keip.flow.web.translation.TranslationController.FLOW_TO_XML_ENDPOINT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties =
        """
                web.cors.enabled = true
                web.cors.allowed-origins[0] = http://localhost:8888
                web.cors.allowed-methods = GET,POST
                """)
class CorsConfigurationAppTest {

  private static final String ORIGIN = "http://localhost:8888";

  @Autowired private MockMvc mockMvc;

  @Test
  void testCorsHeadersIncluded() throws Exception {
    mockMvc
        .perform(
            post(FLOW_TO_XML_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .content(readFlowJson("sample-flow.json"))
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().stringValues(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN))
        .andReturn();
  }
}
