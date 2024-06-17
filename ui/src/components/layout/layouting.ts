import { Edge, Position } from "reactflow"
import { EipFlowNode, LayoutOrientation, GraphConstraint } from "../../api/flow"
import dagre from "@dagrejs/dagre"

const NODE_WIDTH = 128
const NODE_HEIGHT = 128

const graph = new dagre.graphlib.Graph()
graph.setDefaultEdgeLabel(() => ({}))

export const newFlowLayout = (
  nodes: EipFlowNode[],
  edges: Edge[],
  layout: LayoutOrientation,
  constraint: GraphConstraint
) => {
  const direction = layout === "horizontal" ? "LR" : "TB"

  const isHorizontal = layout === "horizontal"

  const newConstraint = constraint === "narrow" ? "narrow" : "wide"

  let seperation

  if (newConstraint === "narrow") {
    seperation = 50
  } else {
    seperation = 150
  }

  graph.setGraph({
    rankdir: direction,
    edgesep: seperation,
    ranksep: seperation,
  })

  nodes.forEach((node) => {
    graph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT })
  })

  edges.forEach((edge) => {
    graph.setEdge(edge.source, edge.target)
  })

  dagre.layout(graph)

  const newNodes = nodes.map((node) => {
    const positionedNode = graph.node(node.id)
    return {
      ...node,
      targetPosition: isHorizontal ? Position.Left : Position.Top,
      sourcePosition: isHorizontal ? Position.Right : Position.Bottom,
      position: {
        x: positionedNode.x - NODE_WIDTH / 2,
        y: positionedNode.y - NODE_HEIGHT / 2,
      },
    }
  })

  return newNodes
}
