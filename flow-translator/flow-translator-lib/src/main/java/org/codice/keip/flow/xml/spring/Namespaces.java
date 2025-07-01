package org.codice.keip.flow.xml.spring;

import org.codice.keip.flow.xml.NamespaceSpec;

public class Namespaces {

  static final NamespaceSpec BEANS =
      new NamespaceSpec(
          "beans",
          "http://www.springframework.org/schema/beans",
          "https://www.springframework.org/schema/beans/spring-beans.xsd");

  static final NamespaceSpec INTEGRATION =
      new NamespaceSpec(
          "integration",
          "http://www.springframework.org/schema/integration",
          "https://www.springframework.org/schema/integration/spring-integration.xsd");
}
