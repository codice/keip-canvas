package org.codice.keip.xsd.xml

import org.codice.keip.xsd.model.eip.ChildGroup
import org.codice.keip.xsd.model.eip.EipChildElement
import org.codice.keip.xsd.model.eip.EipId
import org.codice.keip.xsd.model.eip.Indicator
import org.codice.keip.xsd.model.eip.Occurrence
import spock.lang.Specification

class ChildGroupReducerTest extends Specification {

    def id1 = new EipId("test1", "c1")
    def id2 = new EipId("test2", "c2")
    def id3 = new EipId("test1", "c3")
    def id4 = new EipId("test1", "c4")

    def reducer = new ChildGroupReducer()

    def "Sibling groups with the same child set and indicators are deduplicated"() {
        given:
        def child1 = new EipChildElement.Builder(id1).build()
        def child2 = new EipChildElement.Builder(id2).build()
        def middle1 = new ChildGroup(Indicator.SEQUENCE, [child1, child2])
        def middle2 = new ChildGroup(Indicator.SEQUENCE, [child2, child1])
        def parent = new ChildGroup(Indicator.ALL, [middle1, middle2])

        when:
        def result = reducer.removeRedundantGroups(parent)

        then:
        result.children().size() == 1
        result.children()[0] == middle1
        result.occurrence() == parent.occurrence()
        result.indicator() == parent.indicator()
    }

    def "Sibling groups with the same child set but different indicators are unchanged"() {
        given:
        def child1 = new EipChildElement.Builder(id1).build()
        def child2 = new EipChildElement.Builder(id2).build()
        def middle1 = new ChildGroup(Indicator.SEQUENCE, [child1, child2])
        def middle2 = new ChildGroup(Indicator.CHOICE, [child2, child1])
        def parent = new ChildGroup(Indicator.ALL, [middle1, middle2])

        when:
        def result = reducer.removeRedundantGroups(parent)

        then:
        result == parent
    }

    def "Sibling groups with the same indicators but a mixed (group + element) child set are unchanged"() {
        given:
        def child1 = new EipChildElement.Builder(id1).build()
        def child2 = new EipChildElement.Builder(id2).build()
        def childGroup = new ChildGroup(Indicator.SEQUENCE, []);
        def middle1 = new ChildGroup(Indicator.SEQUENCE, [child1, child2, childGroup])
        def middle2 = new ChildGroup(Indicator.SEQUENCE, [child2, child1, childGroup])
        def parent = new ChildGroup(Indicator.ALL, [middle1, middle2])

        when:
        def result = reducer.removeRedundantGroups(parent)

        then:
        result == parent
    }

    def "Group with a single child is collapsed"() {
        given:
        def child = new EipChildElement.Builder(id1).build()
        def middleOccurrence = new Occurrence(0, 3)
        def middle = new ChildGroup(Indicator.ALL, middleOccurrence, List.of(child))
        def parent = new ChildGroup(Indicator.SEQUENCE, List.of(middle))

        when:
        def result = reducer.collapseSingleChildGroup(parent)

        then:
        parent.children().size() == 1
        result.children()[0] == child
        child.occurrence() == middleOccurrence
        result.occurrence() == parent.occurrence()
        result.indicator() == parent.indicator()
    }

    def "Group with multiple children unchanged"() {
        given:
        def child1 = new EipChildElement.Builder(id1).build()
        def child2 = new EipChildElement.Builder(id2).build()
        def middle = new ChildGroup(Indicator.ALL, [child1, child2])
        def parent = new ChildGroup(Indicator.SEQUENCE, [middle])

        when:
        def result = reducer.collapseSingleChildGroup(parent)

        then:
        result == parent
    }

    def "Group with same indicator child group is collapsed"(Occurrence middleOccur, Occurrence childOccur, Occurrence resultOccur) {
        given:
        def child1 = new EipChildElement.Builder(id1).occurrence(childOccur).build()
        def child2 = new EipChildElement.Builder(id2).occurrence(childOccur).build()
        def middle = new ChildGroup(Indicator.SEQUENCE, middleOccur, [child1, child2])
        def parent = new ChildGroup(Indicator.SEQUENCE, [middle])

        when:
        def result = reducer.collapseSameIndicatorGroups(parent)

        then:
        result.children() == [child1, child2]
        child1.occurrence() == resultOccur
        child2.occurrence() == resultOccur
        result.indicator() == parent.indicator()
        result.occurrence() == result.occurrence()

        where:
        middleOccur                             | childOccur           | resultOccur
        new Occurrence(0, Occurrence.UNBOUNDED) | new Occurrence(1, 1) | new Occurrence(0,
                Occurrence.UNBOUNDED)
        new Occurrence(0, 1)                    | new Occurrence(1, 5) | new Occurrence(0, 5)
        new Occurrence(1, 3)                    | new Occurrence(0, 1) | new Occurrence(0, 3)
        new Occurrence(1, 1)                    | new Occurrence(0, 2) | new Occurrence(0, 2)
    }

    def "CHOICE group with a CHOICE child group is a special case and is not collapsed by the same indicator reducer"() {
        given:
        def child1 = new EipChildElement.Builder(id1).build()
        def child2 = new EipChildElement.Builder(id2).build()
        def middle = new ChildGroup(Indicator.CHOICE, [child1, child2])
        def parent = new ChildGroup(Indicator.CHOICE, [middle])

        when:
        def result = reducer.collapseSameIndicatorGroups(parent)

        then:
        result == parent
    }

    def "Sibling elements with the same name are deduplicated"() {
        given:
        def child1 = new EipChildElement.Builder(id1).build()
        def child2 = new EipChildElement.Builder(id2).build()
        def child3 = new EipChildElement.Builder(id1).build()
        def parent = new ChildGroup(Indicator.ALL, [child1, child2, child3])

        when:
        def result = reducer.deDuplicateElements(parent)

        then:
        result.children() == [child1, child2]
    }

    def "Groups with CHOICE indicator are collapsed"() {
        given:
        def child1 = new EipChildElement.Builder(id1).occurrence(new Occurrence(1, 2)).build()
        def child2 = new EipChildElement.Builder(id2).occurrence(new Occurrence(1, 2)).build()
        def middle = new ChildGroup(Indicator.CHOICE, [child1, child2])
        def parent = new ChildGroup(Indicator.SEQUENCE, [middle])

        def expectedOccurrence = new Occurrence(0, 2)

        when:
        def result = reducer.collapseChoiceGroups(parent)

        then:
        result.children() == [child1, child2]
        child1.occurrence() == expectedOccurrence
        child1.occurrence() == expectedOccurrence
    }

    def "Test full child group reduce"() {
        given:
        def element1 = new EipChildElement.Builder(id1).build()
        def element2 = new EipChildElement.Builder(id2).build()
        def element3 = new EipChildElement.Builder(id3).build()
        def element4 = new EipChildElement.Builder(id4).build()
        def group1 = new ChildGroup(Indicator.SEQUENCE, [element1, element2])
        def group2 = new ChildGroup(Indicator.SEQUENCE, [element2, element1])
        def group3 = new ChildGroup(Indicator.CHOICE, [element3])
        def group4 = new ChildGroup(Indicator.SEQUENCE, [element4])
        def top = new ChildGroup(Indicator.SEQUENCE, [group1, group2, group3, group4, element2])

        when:
        def result = reducer.reduceGroup(top)

        then:
        result.children().collect { it.getEipId() } == [id1, id2, id3, id4]
    }

    def "Circular references from descendants to ancestors are broken"() {
        given:
        def top = new EipChildElement.Builder(id1).build()
        def middle = new EipChildElement.Builder(id2).build()
        def bottom = new EipChildElement.Builder(id3).build()

        top.addChild(new ChildGroup(Indicator.SEQUENCE, [middle]))
        middle.addChild(new ChildGroup(Indicator.SEQUENCE, [bottom]))
        // create cycle
        bottom.addChild(new ChildGroup(Indicator.SEQUENCE, [top]))

        def rootGroup = new ChildGroup(Indicator.SEQUENCE, [top])

        when:
        def result = reducer.reduceGroup(rootGroup)

        then:
        def topResult = (result.children()[0] as EipChildElement)
        topResult.getEipId() == top.getEipId()

        def middleResult = (topResult.children()[0] as EipChildElement)
        middleResult.getEipId() == middle.getEipId()

        def bottomResult = (middleResult.children()[0] as EipChildElement)
        bottomResult.getEipId() == bottom.getEipId()

        def backRef = (bottomResult.children()[0] as EipChildElement)
        backRef.getEipId() == top.getEipId()
        backRef.getChildGroup() == null
    }

    def "Terminate traversal when max child nesting depth is reached"() {
        given:
        def first = new EipChildElement.Builder(id1).build()
        def second = new EipChildElement.Builder(id2).build()
        def third = new EipChildElement.Builder(id3).build()
        def fourth = new EipChildElement.Builder(id4).build()

        first.addChild(new ChildGroup(Indicator.SEQUENCE, [second]))
        second.addChild(new ChildGroup(Indicator.SEQUENCE, [third]))
        third.addChild(new ChildGroup(Indicator.SEQUENCE, [fourth]))

        def rootGroup = new ChildGroup(Indicator.SEQUENCE, [first])

        when:
        def result = new ChildGroupReducer(2).reduceGroup(rootGroup)

        then:
        def thirdResult = result.children()[0].children()[0].children()[0] as EipChildElement
        thirdResult.getChildGroup() == null
    }
}
