package org.codice.keip.flow.web;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FlowTranslatorAppTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void contextLoads() {}

  @Test
  void testCorsHeadersExcludedByDefault() throws Exception {
    mockMvc
        .perform(
            post("/").contentType(APPLICATION_JSON_VALUE).content(readFlowJson("sample-flow.json")))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.VARY));
  }

  static String readFlowJson(String filename) throws IOException {
    Path path = Path.of("json").resolve(filename);
    try (var inputStream =
        CorsConfigurationAppTest.class.getClassLoader().getResourceAsStream(path.toString())) {
      assert inputStream != null;
      return new String(inputStream.readAllBytes());
    }
  }
}
