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
  customEntities: Record<string, string>
  edges: CustomEdge[]
  eipConfigs: Record<string, EipConfig>
  layout: Layout
  nodes: CustomNode[]
  selectedChildNode: string[] | null
}

export interface SerializedFlow {
  nodes: AppStore["nodes"]
  edges: AppStore["edges"]
  eipConfigs: AppStore["eipConfigs"]
  version: string

  customEntities?: AppStore["customEntities"]
}
