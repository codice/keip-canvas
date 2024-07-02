import dagre from "@dagrejs/dagre"
import { Edge, Position } from "reactflow"
import { EipFlowNode, Layout } from "../../api/flow"


const DEFAULT_NODE_WIDTH = 128
const DEFAULT_NODE_HEIGHT = 128

const graph = new dagre.graphlib.Graph()
graph.setDefaultEdgeLabel(() => ({}))

export const newFlowLayout = (
  nodes: EipFlowNode[],
  edges: Edge[],
  layout: Layout
) => {
  const direction = layout.orientation === "horizontal" ? "LR" : "TB"

  const isHorizontal = layout.orientation === "horizontal"

  let rankSeperation
  let nodeSeperation

  if (layout.density === "compact") {
    rankSeperation = 20
    nodeSeperation = 20
  } else if (layout.density === "comfortable") {
    rankSeperation = 75
    nodeSeperation = 75
  }

  graph.setGraph({
    rankdir: direction,
    ranksep: rankSeperation,
    nodesep: nodeSeperation,
  })

  nodes.forEach((node) => {
    graph.setNode(node.id, { width: getWidth(node), height: getHeight(node) })
  })

  edges.forEach((edge) => {
    edge.type = "simplebezier"
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
        x: positionedNode.x - getWidth(node) / 2,
        y: positionedNode.y - getHeight(node) / 2,
      },
    }
  })

  return newNodes
}

const getHeight = (node: EipFlowNode) => node.height ?? DEFAULT_NODE_HEIGHT
const getWidth = (node: EipFlowNode) => node.width ?? DEFAULT_NODE_WIDTH
