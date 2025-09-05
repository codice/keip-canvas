package org.codice.keip.xsd.test

import org.codice.keip.xsd.model.eip.Attribute
import org.codice.keip.xsd.model.eip.ChildComposite
import org.codice.keip.xsd.model.eip.ChildGroup
import org.codice.keip.xsd.model.eip.EipChildElement
import org.codice.keip.xsd.model.eip.EipComponent

import java.util.function.BiConsumer

class EipComparisonUtils {
    static void assertCollectionsEqualNoOrder(
            Collection expected,
            Collection actual,
            Comparator comparator,
            BiConsumer assertion,
            String message) {
        if (expected == null && actual == null) {
            return
        }

        assert expected.size() == actual.size(): message

        def expectedSort = expected.sort(false, comparator)
        def actualSort = actual.sort(false, comparator)

        for (i in 0..<expected.size()) {
            assertion.accept(expectedSort[i], actualSort[i])
        }
    }

    static void assertEipComponentsEqual(EipComponent expected, EipComponent actual) {
        assert expected.getName() == actual.getName()
        assert expected.getRole() == actual.getRole()
        assert expected.getConnectionType() == actual.getConnectionType()
        assert expected.getDescription() == actual.getDescription()
        assertCollectionsEqualNoOrder(
                expected.getAttributes(),
                actual.getAttributes(),
                Comparator.comparing(Attribute::name),
                { exp, act -> assert exp == act },
                String.format(
                        "Comparing EIP Component Attributes (component: %s)", expected.getName()))
        assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup())
    }

    static void assertEipChildGroupsEqual(ChildComposite expected, ChildComposite actual) {
        if (expected == null && actual == null) {
            return
        }

        assert expected instanceof ChildGroup
        assert actual instanceof ChildGroup

        ChildGroup expectedGroup = expected
        ChildGroup actualGroup = actual

        List<EipChildElement> expectedChildren = expectedGroup.children() as List<EipChildElement>

        List<EipChildElement> actualChildren = actualGroup.children() as List<EipChildElement>

        assert expectedGroup.indicator() == actualGroup.indicator()
        assert expectedGroup.occurrence() == actual.occurrence()
        assertCollectionsEqualNoOrder(
                expectedChildren,
                actualChildren,
                Comparator.comparing(EipChildElement::getName),
                EipComparisonUtils::assertEipChildElementsEqual,
                "Comparing Child Groups")
    }


    static void assertEipChildElementsEqual(EipChildElement expected, EipChildElement actual) {
        assert expected.getName() == actual.getName()
        assert expected.getDescription() == actual.getDescription()
        assert expected.occurrence() == actual.occurrence()
        assertCollectionsEqualNoOrder(
                expected.getAttributes(),
                actual.getAttributes(),
                Comparator.comparing(Attribute::name),
                { exp, act -> assert exp == act },
                String.format("Comparing child elements (child-name: %s)", expected.getName()))
        assertEipChildGroupsEqual(expected.getChildGroup(), actual.getChildGroup())
    }
}
