import { Node } from "reactflow"
import { EipId } from "./id"

export interface EipNodeData {
  eipId: EipId
  label: string
}

export type EipFlowNode = Node<EipNodeData>

export type LayoutOrientation = "horizontal" | "vertical" 

export type GraphConstraint = "narrow" | "wide"

export const EIP_NODE_KEY = "eipNode"
