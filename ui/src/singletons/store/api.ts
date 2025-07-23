import { CustomEdge, CustomNode, Layout, RouterKey } from "../../api/flow"
import { Attributes, EipId } from "../../api/generated/eipFlow"

export interface EipConfig {
  attributes: Attributes
  children: string[]
  eipId: EipId
  description?: string
  routerKey?: RouterKey
}

export interface AppStore {
  nodes: CustomNode[]
  edges: CustomEdge[]
  eipConfigs: Record<string, EipConfig>
  selectedChildNode: string[] | null
  layout: Layout
}

export interface SerializedFlow {
  nodes: AppStore["nodes"]
  edges: AppStore["edges"]
  eipConfigs: AppStore["eipConfigs"]
  version: string
}
