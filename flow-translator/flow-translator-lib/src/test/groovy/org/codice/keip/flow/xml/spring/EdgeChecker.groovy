package org.codice.keip.flow.xml.spring

import org.codice.keip.flow.model.EipGraph
import org.codice.keip.flow.model.EipNode

class EdgeChecker {
    private EipGraph graph

    EdgeChecker(EipGraph graph) {
        this.graph = graph
    }

    void check(EipNode node, List<EipNode> predecessors, List<EipNode> successors) {
        assert graph.predecessors(node) == predecessors.toSet()
        assert graph.successors(node) == successors.toSet()
    }
}
