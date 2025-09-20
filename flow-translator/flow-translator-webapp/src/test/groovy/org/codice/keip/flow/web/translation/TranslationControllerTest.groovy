package org.codice.keip.flow.web.translation

import com.fasterxml.jackson.databind.json.JsonMapper
import org.codice.keip.flow.model.ConnectionType
import org.codice.keip.flow.model.EipId
import org.codice.keip.flow.model.EipNode
import org.codice.keip.flow.model.Flow
import org.codice.keip.flow.model.Role
import org.codice.keip.flow.web.config.JacksonMapperConfig
import org.codice.keip.flow.web.error.ApiError
import org.codice.keip.flow.web.error.DefaultErrorResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import javax.xml.transform.TransformerException
import java.nio.file.Path

import static org.codice.keip.flow.web.translation.TranslationController.FLOW_TO_XML_ENDPOINT
import static org.codice.keip.flow.web.translation.TranslationController.XML_TO_FLOW_ENDPOINT
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = TranslationController)
@Import(JacksonMapperConfig.class)
class TranslationControllerTest extends Specification {

    private static final JsonMapper MAPPER = JsonMapper.builder().build()

    private static final String OUTPUT_XML = "<test>canned</test>"

    private static final String OUTPUT_FLOW = buildOutputFlow()

    @Autowired
    MockMvc mvc

    @Autowired
    TranslationService translationService

    def "valid flow json to XML -> returns ok response with body"() {
        given:
        def translationResult = new TranslationResponse(OUTPUT_XML, null)
        translationService.toXml(_ as Flow, false) >> translationResult

        expect:
        MvcResult mvcResult = mvc.perform(post(FLOW_TO_XML_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .content(readFlowJson("sample-flow.json")))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, translationResult)
    }

    def "valid flow json to pretty-printed XML -> returns ok response with body"() {
        given:
        def translationResult = new TranslationResponse(OUTPUT_XML, null)
        translationService.toXml(_ as Flow, true) >> translationResult

        expect:
        MvcResult mvcResult = mvc.perform(post(FLOW_TO_XML_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .queryParam("prettyPrint", "true")
                .content(readFlowJson("sample-flow.json")))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, translationResult)
    }

    def "flow json to XML with non-critical transformation errors -> returns error response with partial body"() {
        given:
        def errDetails = new TranslationErrorDetail("node1", "unknown node")
        def err = ApiError.of(new TransformerException("unsupported node type"), [errDetails])
        def translationResult = new TranslationResponse(OUTPUT_XML, err)
        translationService.toXml(_ as Flow, _ as Boolean) >> translationResult

        expect:
        MvcResult mvcResult = mvc.perform(post(FLOW_TO_XML_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .content(readFlowJson("sample-flow.json")))
                                 .andExpect(status().isInternalServerError())
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, translationResult)
    }

    def "flow json to XML with fatal transformation errors -> returns error response with no body"(Exception ex, int httpStatusCode) {
        given:
        translationService.toXml(_ as Flow, _ as Boolean) >> { throw ex }

        expect:
        MvcResult mvcResult = mvc.perform(post(FLOW_TO_XML_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .content(readFlowJson("sample-flow.json")))
                                 .andExpect(status().is(httpStatusCode))
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, new TranslationResponse(null, ApiError.of(ex)))

        where:
        ex                                        | httpStatusCode
        new IllegalArgumentException("bad input") | HttpStatus.BAD_REQUEST.value()
        new RuntimeException("unknown")           | HttpStatus.INTERNAL_SERVER_ERROR.value()
    }

    def "malformed flow json -> deserialization error -> returns error response with no body"() {
        given:
        def translationResult = new TranslationResponse(OUTPUT_XML, null)
        translationService.toXml(_ as Flow, _ as Boolean) >> translationResult

        expect:
        MvcResult mvcResult = mvc.perform(post(FLOW_TO_XML_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .content(readFlowJson("invalid-role-flow.json")))
                                 .andExpect(status().isBadRequest())
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()
        // Verify expected API error format is returned
        MAPPER.readValue(mvcResult.getResponse().getContentAsString(), DefaultErrorResponse.class)
    }

    def "valid XML to flow json -> returns ok response with body"() {
        given:
        def translationResult = new TranslationResponse(OUTPUT_FLOW, null)
        translationService.fromXml(_ as InputStream) >> translationResult

        expect:
        MvcResult mvcResult = mvc.perform(post(XML_TO_FLOW_ENDPOINT)
                .contentType(APPLICATION_XML_VALUE)
                .content(readXml("formatted-sample.xml")))
                                 .andExpect(status().isOk())
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, translationResult)
    }

    def "XML to flow json with fatal transformation errors -> returns error response with no body"(Exception ex, int httpStatusCode) {
        given:
        translationService.fromXml(_ as InputStream) >> { throw ex }

        expect:
        MvcResult mvcResult = mvc.perform(post(XML_TO_FLOW_ENDPOINT)
                .contentType(APPLICATION_XML_VALUE)
                .content(readXml("formatted-sample.xml")))
                                 .andExpect(status().is(httpStatusCode))
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, new TranslationResponse(null, ApiError.of(ex)))

        where:
        ex                                        | httpStatusCode
        new IllegalArgumentException("bad input") | HttpStatus.BAD_REQUEST.value()
        new RuntimeException("unknown")           | HttpStatus.INTERNAL_SERVER_ERROR.value()
        new IOException("stream error")           | HttpStatus.INTERNAL_SERVER_ERROR.value()
    }

    def "XML to flow json with non-critical transformation errors -> returns error response with partial body"() {
        given:
        def errDetails = new TranslationErrorDetail("node1", "unknown node")
        def err = ApiError.of(new TransformerException("unsupported node type"), [errDetails])
        def translationResult = new TranslationResponse(OUTPUT_FLOW, err)
        translationService.fromXml(_ as InputStream) >> translationResult

        expect:
        MvcResult mvcResult = mvc.perform(post(XML_TO_FLOW_ENDPOINT)
                .contentType(APPLICATION_XML_VALUE)
                .content(readXml("formatted-sample.xml")))
                                 .andExpect(status().isInternalServerError())
                                 .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                                 .andReturn()

        verifyTranslationResult(mvcResult, translationResult)
    }

    private static void verifyTranslationResult(MvcResult actual, Object expected) {
        def responseJson = actual.getResponse().getContentAsString()
        assert MAPPER.readValue(responseJson, TranslationResponse.class) == expected
    }

    private static String readXml(String filename) {
        Path path = Path.of("xml").resolve(filename)
        return TranslationControllerTest.class.getClassLoader()
                                        .getResource(path.toString()).text
    }

    private static Flow buildOutputFlow() {
        def test = new EipNode("test",
                new EipId("testspace", "converter"),
                null,
                "testnode",
                Role.TRANSFORMER,
                ConnectionType.PASSTHRU,
                ["k1": "v1"],
                [])
        return new Flow([test], [])
    }

    static String readFlowJson(String filename) {
        Path path = Path.of("json").resolve(filename)
        return TranslationControllerTest.class.getClassLoader()
                                        .getResource(path.toString()).text
    }

    @TestConfiguration
    static class MockConfig {
        def detachedMockFactory = new DetachedMockFactory()

        @Bean
        TranslationService translationService() {
            return detachedMockFactory.Stub(TranslationService)
        }
    }
}
