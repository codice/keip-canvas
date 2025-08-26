package org.codice.keip.flow

class ComponentRegistryIO {

    // Provided by adding a test-scope dependency on the `eip-schema-definitions` artifact
    private static final String SI_EIP_COMPONENTS_JSON_PATH = "/springIntegrationEipComponents.json"

    static InputStream readComponentDefinitionJson() {
        return ComponentRegistryIO.class.getResourceAsStream(SI_EIP_COMPONENTS_JSON_PATH)
    }
}
