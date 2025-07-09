import { BuiltInEdge, Edge, Node } from "@xyflow/react"
import { Attribute } from "./generated/eipComponentDef"
import { Attributes } from "./generated/eipFlow"

export interface Layout {
  orientation: "horizontal" | "vertical"
  density: "compact" | "comfortable"
}

export enum CustomNodeType {
  EipNode = "eipNode",
  FollowerNode = "followerNode",
}

// react flow requires using a type rather than an interface for NodeData
// eslint-disable-next-line @typescript-eslint/consistent-type-definitions
export type EipNodeData = {
  label?: string

  // The id of a follower that is linked to this one (i.e. has the same lifecycle)
  followerId?: string
}

export type EipFlowNode = Node<EipNodeData, CustomNodeType.EipNode>

export const isEipNode = (node?: Node): node is EipFlowNode =>
  node?.type === CustomNodeType.EipNode

// react flow requires using a type rather than an interface for NodeData
// eslint-disable-next-line @typescript-eslint/consistent-type-definitions
export type FollowerNodeData = {
  leaderId: string
}

export type FollowerNode = Node<FollowerNodeData, CustomNodeType.FollowerNode>

export const isFollowerNode = (node?: Node): node is FollowerNode =>
  node?.type === CustomNodeType.FollowerNode

export type CustomNode = EipFlowNode | FollowerNode

export interface ChannelMapping {
  mapperName: string
  matcher: Attribute
  matcherValue?: string
  isDefaultMapping?: boolean
}

export const DYNAMIC_EDGE_TYPE = "dynamicEdge"

// TODO: Should custom edge data contain only a reference to the mapping?
// The mapping could be stored separately in the app store.
//
// react flow requires using a type rather than an interface for EdgeData
// eslint-disable-next-line @typescript-eslint/consistent-type-definitions
export type DynamicEdgeData = {
  mapping: ChannelMapping
}

export type DynamicEdge = Edge<DynamicEdgeData, typeof DYNAMIC_EDGE_TYPE>

export type CustomEdge = BuiltInEdge | DynamicEdge

export const isDynamicEdge = (edge?: Edge): edge is DynamicEdge =>
  edge?.type === DYNAMIC_EDGE_TYPE

export interface RouterKeyDef {
  name: string
  type: "attribute" | "child"
  attributesDef: Attribute[]
}

export interface RouterKey {
  name: string
  attributes?: Attributes
}

export const DEFAULT_NAMESPACE = "integration"
