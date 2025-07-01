package org.codice.keip.flow.web.cors

import org.springframework.web.servlet.config.annotation.CorsRegistration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import spock.lang.Specification

class CorsConfigurationTest extends Specification {

    CorsRegistration registration = Mock()

    CorsRegistry registry = Stub() {
        addMapping("/**") >> registration
    }

    def "Cors is enabled with no extra configuration"() {
        given:
        def props = new CorsProps(null, null)

        when:
        def cors = new CorsConfiguration(props)
        cors.configureCorsMapping().addCorsMappings(registry)

        then:
        0 * registration.allowedOrigins(_)
        0 * registration.allowedMethods(_)
    }

    def "Cors is enabled with full configuration"() {
        given:
        String[] origins = ["http://localhost:8000", "http://localhost:8001"]
        String[] methods = ["POST,PUT"]
        def props = new CorsProps(origins, methods)

        when:
        def cors = new CorsConfiguration(props)
        cors.configureCorsMapping().addCorsMappings(registry)

        then:
        1 * registration.allowedOrigins(origins)
        1 * registration.allowedMethods(methods)
    }

}
