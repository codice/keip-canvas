import { BuiltInEdge, Connection } from "@xyflow/react"
import isDeepEqual from "fast-deep-equal"
import {
  CustomEdge,
  CustomNode,
  CustomNodeType,
  DynamicEdge,
  DynamicEdgeData,
  EipFlowNode,
  FollowerNode,
  RouterKeyDef,
} from "../../api/flow"
import {
  Attributes,
  EipChildNode,
  EipFlow,
  EipNode,
  FlowEdge,
} from "../../api/generated/eipFlow"
import { newFlowLayout } from "../../components/layout/layouting"
import { generateChildId } from "../../utils/nodeIdGenerator"
import {
  DYNAMIC_ROUTING_CHILDREN,
  eipIdToString,
  lookupContentBasedRouterKeys,
  lookupEipComponent,
} from "../eipDefinitions"
import { describeFollower, FollowerNodeDescriptor } from "../followerNodeDefs"
import { AppStore, EipConfig } from "./api"
import { newEipNode } from "./appActions"
import { createDynamicRoutingEdge } from "./reactFlowActions"
import { getLayoutView } from "./storeViews"

/**
 * Converts an {@link EipFlow} to a representation suitable for importing into the {@link AppStore}.
 * This function performs the inverse transformation of the `diagramToEipFlow` function.
 *
 *
 * @param flow the EipFlow to convert
 * @returns An AppStore importable object
 */
export const eipFlowToDiagram = (flow: EipFlow): Partial<AppStore> => {
  /*
  The initial transformation (creating `EipFlowNodes` and `DefaultEdges`) is straightforward.
  Much of the complexity arises from handling of the content-based routers and leader/follower
  (e.g. inbound-gateways) special cases.
  */

  if (!flow.nodes) {
    throw new Error("The provided EipFlow does not contain any nodes")
  }

  const eipConfigs: AppStore["eipConfigs"] = {}

  const nodeLabelToIdMap = new Map<string, string>()

  // Initial transformations
  const nodes = flow.nodes.map((node) =>
    toEipFlowNode(node, eipConfigs, nodeLabelToIdMap)
  )

  const edges = flow.edges?.map((flowEdge) =>
    toDefaultEdge(flowEdge, nodeLabelToIdMap)
  )

  // Maps a node id to a filtered list of its children that
  // contain routing information (e.g. 'mapping' or 'recipient')
  const routingChildren = new Map<string, EipConfig[]>()

  // Process content-based router nodes
  nodes.forEach((node) => {
    const nodeConfig = eipConfigs[node.id]
    const eipId = nodeConfig.eipId
    const eipComponent = lookupEipComponent(eipId)

    if (eipComponent?.connectionType !== "content_based_router") {
      return
    }

    updateContentRouterNode(node, eipConfigs, routingChildren)
  })

  // Process dynamic edges
  const withDynamicEdges: CustomEdge[] = []
  edges?.forEach((edge) => {
    // Is this edge exiting a content-based router node?
    if (routingChildren.has(edge.source)) {
      withDynamicEdges.push(toDynamicEdge(edge, eipConfigs, routingChildren))
      return
    }

    withDynamicEdges.push(edge)
  })

  // Process leader/follower nodes and edges
  const { followerEdgeMatchers, nodeReferenceMap } = findLeaderNodes(
    nodes,
    eipConfigs
  )
  const filteredEdges = processFollowerEdges(
    withDynamicEdges,
    followerEdgeMatchers,
    nodeReferenceMap
  )

  // Generate diagram layout
  let positionedNodes: CustomNode[] = nodes
  if (edges) {
    positionedNodes = newFlowLayout(nodes, edges, getLayoutView())
  }

  return {
    customEntities: flow.customEntities,
    nodes: positionedNodes,
    edges: filteredEdges,
    eipConfigs,
  }
}

/**
 * Converts an {@link EipNode}, from an {@link EipFlow}, to an @{@link EipFlowNode}
 * which becomes part of the diagram. In addition, it populates the `eipConfigs` object with
 * the required properties (e.g. attributes, children) from the provided node.
 */
const toEipFlowNode = (
  flowNode: EipNode,
  eipConfigs: AppStore["eipConfigs"],
  nodeLabelToIdMap: Map<string, string>
): EipFlowNode => {
  const eipComponent = lookupEipComponent(flowNode.eipId)
  if (!eipComponent) {
    throw new Error(`Unknown EipId: '${eipIdToString(flowNode.eipId)}'`)
  }

  const diagramNode = newEipNode({ x: 0, y: 0 })
  diagramNode.data.label = flowNode.id
  nodeLabelToIdMap.set(diagramNode.data.label, diagramNode.id)

  const childIds = flowNode.children?.map((c) =>
    collectEipChildren(c, eipConfigs)
  )

  eipConfigs[diagramNode.id] = {
    attributes: flowNode.attributes ?? {},
    children: childIds ?? [],
    eipId: flowNode.eipId,
  }

  return diagramNode
}

/**
 * Maps a {@link FlowEdge} to a default diagram edge.
 */
const toDefaultEdge = (
  flowEdge: FlowEdge,
  nodeLabelToIdMap: Map<string, string>
): BuiltInEdge => {
  const source = nodeLabelToIdMap.get(flowEdge.source)
  const target = nodeLabelToIdMap.get(flowEdge.target)

  if (!source || !target) {
    throw new Error(`Disconnected edge in EipFlow: '${flowEdge.id}'`)
  }

  const edge: BuiltInEdge = {
    id: flowEdge.id,
    source,
    target,
  }

  if (flowEdge.type === "discard") {
    edge.sourceHandle = "discard"
  }

  return edge
}

/**
 * Recursively walks the child tree, adds an eipConfig entry for
 * each child, and returns the child's generated id.
 */
const collectEipChildren = (
  child: EipChildNode,
  eipConfigs: AppStore["eipConfigs"]
) => {
  const childIds = child.children?.map((c) => collectEipChildren(c, eipConfigs))

  const id = generateChildId()
  eipConfigs[id] = {
    eipId: child.eipId,
    attributes: child.attributes ?? {},
    children: childIds ?? [],
  }

  return id
}

/**
 * Handles content-based router nodes:
 *
 * - The node's routing key is added to its `eipConfigs` entry
 * - Children concerned with routing logic (e.g. 'mapping', 'recipient') are extracted
 *   and recorded in the`routingChildren` map.
 */
const updateContentRouterNode = (
  node: EipFlowNode,
  eipConfigs: AppStore["eipConfigs"],
  routingChildren: Map<string, EipConfig[]>
) => {
  const nodeConfig = eipConfigs[node.id]

  const routerKeyDef = lookupContentBasedRouterKeys(nodeConfig.eipId)
  routerKeyDef && addRouterKeyToEipConfig(node.id, routerKeyDef, eipConfigs)

  const { routingChildList, remainingChildren } = filterRoutingChildren(
    nodeConfig.children,
    eipConfigs
  )

  // Add all routing children to 'routingChildren' map
  routingChildList.forEach((id) => {
    const childConfig = eipConfigs[id]
    let configList = routingChildren.get(node.id)
    configList = configList ? [...configList, childConfig] : [childConfig]
    routingChildren.set(node.id, configList)
  })

  nodeConfig.children = remainingChildren
}

/**
 * Edges that exit a content-based router are upgraded to a {@link DynamicEdge}.
 * The mappings stored in `routingChildren` are used to populate the updated
 * edge's `data` field.
 */
const toDynamicEdge = (
  edge: BuiltInEdge,
  eipConfigs: AppStore["eipConfigs"],
  routingChildren: Map<string, EipConfig[]>
): DynamicEdge => {
  const sourceConfig = eipConfigs[edge.source]
  const routerEipId = lookupEipComponent(sourceConfig.eipId)
  if (!routerEipId) {
    throw new Error(
      `The source router has an unregistered eipId: '${eipIdToString(sourceConfig.eipId)}'`
    )
  }

  const dynamicEdge = createDynamicRoutingEdge(
    edge as Connection,
    routerEipId
  ) as DynamicEdge

  // Find the routing child that corresponds to the current edge
  const mappingChildren = routingChildren.get(edge.source)!
  const mapping = mappingChildren.find(
    (config) => config.attributes.channel === edge.id
  )

  updateDynamicEdgeDataMatcher(dynamicEdge.data!, mapping)
  return dynamicEdge
}

const updateDynamicEdgeDataMatcher = (
  data: DynamicEdgeData,
  mappingConfig: EipConfig | undefined
) => {
  if (mappingConfig) {
    const matcherName = data.mapping.matcher.name
    const value = mappingConfig.attributes[matcherName]
    if (value) {
      data.mapping.matcherValue = String(value)
    }
  } else {
    data.mapping.isDefaultMapping = true
  }
}

const filterRoutingChildren = (
  children: string[],
  eipConfigs: AppStore["eipConfigs"]
) => {
  const routingChildList: string[] = []
  const remainingChildren: string[] = []

  children.forEach((childId) => {
    const childConfig = eipConfigs[childId]
    if (DYNAMIC_ROUTING_CHILDREN.has(childConfig.eipId.name)) {
      routingChildList.push(childId)
    } else {
      remainingChildren.push(childId)
    }
  })

  return { routingChildList, remainingChildren }
}

const addRouterKeyToEipConfig = (
  nodeId: string,
  routerKeyDef: RouterKeyDef,
  eipConfigs: AppStore["eipConfigs"]
) => {
  switch (routerKeyDef.type) {
    case "attribute": {
      addAttributeRouterKeyConfig(nodeId, routerKeyDef, eipConfigs)
      break
    }
    case "child": {
      addChildRouterKeyConfig(nodeId, routerKeyDef, eipConfigs)
      break
    }
  }
}

const addAttributeRouterKeyConfig = (
  nodeId: string,
  routerKeyDef: RouterKeyDef,
  eipConfigs: AppStore["eipConfigs"]
) => {
  const nodeConfig = eipConfigs[nodeId]
  const targetAttrs = new Set(routerKeyDef.attributesDef.map((a) => a.name))
  const extractedRoutingAttrs = Object.keys(nodeConfig.attributes)
    .filter((key) => targetAttrs.has(key))
    .reduce((acc, key) => {
      acc[key] = nodeConfig.attributes[key]
      delete nodeConfig.attributes[key]
      return acc
    }, {} as Attributes)

  eipConfigs[nodeId].routerKey = {
    eipId: routerKeyDef.eipId,
    attributes: extractedRoutingAttrs,
  }
}

const addChildRouterKeyConfig = (
  nodeId: string,
  routerKeyDef: RouterKeyDef,
  eipConfigs: AppStore["eipConfigs"]
) => {
  const nodeConfig = eipConfigs[nodeId]
  const routingChildId = nodeConfig.children.find(
    (childId) =>
      eipIdToString(eipConfigs[childId]?.eipId) ===
      eipIdToString(routerKeyDef.eipId)
  )

  if (routingChildId) {
    eipConfigs[nodeId].routerKey = {
      eipId: routerKeyDef.eipId,
      attributes: eipConfigs[routingChildId].attributes,
    }
    delete eipConfigs[routingChildId]
    nodeConfig.children = nodeConfig.children.filter(
      (id) => id !== routingChildId
    )
  }
}

/**
 * Scans the list of nodes for "leader" nodes. Leaders are determined by checking if they have
 * any followers defined according to {@link describeFollower}. Once a leader is identified,
 * an EdgeMatcher is built to identify its follower within the EipFlow.
 */
const findLeaderNodes = (
  nodes: CustomNode[],
  eipConfigs: AppStore["eipConfigs"]
) => {
  const followerEdgeMatchers = new Map<string, (edge: CustomEdge) => boolean>()
  const nodeReferenceMap = new Map<string, CustomNode>()

  nodes.forEach((node) => {
    nodeReferenceMap.set(node.id, node)
    const nodeConfig = eipConfigs[node.id]
    const descriptor = describeFollower(nodeConfig.eipId)
    if (descriptor) {
      const hiddenEdge = descriptor.hiddenEdge?.(node.id, "")

      const hiddenEdgeMatcher = getHiddenEdgeMatcher(
        hiddenEdge,
        node.id,
        descriptor,
        eipConfigs
      )

      hiddenEdgeMatcher && followerEdgeMatchers.set(node.id, hiddenEdgeMatcher)
    }
  })

  return { followerEdgeMatchers, nodeReferenceMap }
}

/**
 * Iterates through the list of edges and checks if it's a follower edge
 * (connects a leader and a follower). If so, the edge is removed from the list,
 * and `id` references are added to both leader and follower node data.
 *
 * @returns a filtered list of edges, excluding any hidden follower edges.
 */
const processFollowerEdges = (
  edges: CustomEdge[],
  followerEdgeMatchers: Map<string, (edge: CustomEdge) => boolean>,
  nodeReferenceMap: Map<string, CustomNode>
) => {
  const filteredEdges: CustomEdge[] = []

  edges.forEach((edge) => {
    const ids = getIdsFromLeaderEdge(edge, followerEdgeMatchers)
    if (ids) {
      const { leaderId, followerId } = ids
      const isHiddenEdge = followerEdgeMatchers.get(leaderId)!
      if (isHiddenEdge(edge)) {
        updateNodeReferences(leaderId, followerId, nodeReferenceMap)

        // exclude hidden edge
        return
      }
    }

    filteredEdges.push(edge)
  })

  return filteredEdges
}

const updateNodeReferences = (
  leaderId: string,
  followerId: string,
  nodeReferenceMap: Map<string, CustomNode>
) => {
  const leaderNode = nodeReferenceMap.get(leaderId) as EipFlowNode
  if (leaderNode) {
    leaderNode.data.followerId = followerId
  }

  const followerNode = nodeReferenceMap.get(followerId) as FollowerNode
  if (followerNode) {
    followerNode.type = CustomNodeType.FollowerNode
    followerNode.data = { leaderId }
  }
}

/**
 * The returned function can be applied to a {@link CustomEdge} to determine
 * if it is a leader-follower edge.
 */
const getHiddenEdgeMatcher = (
  edge: Partial<CustomEdge> | undefined,
  leaderId: string,
  descriptor: FollowerNodeDescriptor,
  eipConfigs: AppStore["eipConfigs"]
) => {
  if (!edge) {
    return null
  }

  if (edge.source === leaderId) {
    // leader -> follower edge
    return (edge: CustomEdge) =>
      edge.source === leaderId &&
      isDeepEqual(eipConfigs[edge.target].eipId, descriptor.eipId)
  }

  // follower -> leader edge
  return (edge: CustomEdge) =>
    edge.target === leaderId &&
    isDeepEqual(eipConfigs[edge.source].eipId, descriptor.eipId)
}

const getIdsFromLeaderEdge = (
  edge: CustomEdge,
  leaderNodes: Map<string, object>
) => {
  if (leaderNodes.has(edge.source)) {
    return { leaderId: edge.source, followerId: edge.target }
  } else if (leaderNodes.has(edge.target)) {
    return { leaderId: edge.target, followerId: edge.source }
  } else {
    return null
  }
}
