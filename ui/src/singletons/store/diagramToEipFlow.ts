import isDeepEqual from "fast-deep-equal"
import { useStoreWithEqualityFn } from "zustand/traditional"
import {
  CustomEdge,
  CustomNode,
  CustomNodeType,
  DynamicEdge,
  EipFlowNode,
  isDynamicEdge,
  isFollowerNode,
  RouterKey,
} from "../../api/flow"
import {
  Attributes,
  EipChildNode,
  EipFlow,
  EipId,
  EipNode,
  FlowEdge,
} from "../../api/generated/eipFlow"
import {
  CHANNEL_ATTR_NAME,
  DEFAULT_OUTPUT_CHANNEL_NAME,
  lookupContentBasedRouterKeys,
  lookupEipComponent,
} from "../eipDefinitions"
import { describeFollower, describeFollowerFromId } from "../followerNodeDefs"
import { AppStore, EipConfig } from "./api"
import { useAppStore } from "./appStore"
import { childrenBreadthTraversal, getEipId } from "./storeViews"

const EIP_NAMESPACE_TO_XML_PREFIX: Record<string, string> = {
  xml: "int-xml",
  "web-services": "ws",
}

export const useEipFlow = () =>
  useStoreWithEqualityFn(
    useAppStore,
    (state) => diagramToEipFlow(state),
    isDeepEqual
  )

// TODO: Dynamic router logic is over-complicating this method, extract to
// a separate method and apply modifications after building original flow.
const diagramToEipFlow = (state: AppStore): EipFlow => {
  const nodeLookup = createNodeLookupMap(state.nodes)

  const routerChildMap = new Map<string, EipChildNode[]>()
  const routerAttrMap = new Map<string, Attributes>()

  const allEdges = addHiddenFollowerEdges(state.nodes, state.edges)

  const edges: FlowEdge[] = allEdges.map((edge) => {
    const source = getNodeId(edge.source, nodeLookup)
    const target = getNodeId(edge.target, nodeLookup)
    const edgeId = `ch-${source}-${target}`

    if (isDynamicEdge(edge)) {
      addRouterChannelMapping(edgeId, edge, routerChildMap, routerAttrMap)
    }

    return {
      id: edgeId,
      source,
      target,
      type: edge.sourceHandle === "discard" ? "discard" : "default",
    }
  })

  const nodes: EipNode[] = state.nodes.map((node) => {
    const eipId = getEipId(node.id)!
    const eipComponent = lookupEipComponent(eipId)!
    const namespace = eipId.namespace

    const routerKey = state.eipConfigs[node.id].routerKey
    const routerKeyAttrs =
      eipComponent.connectionType === "content_based_router" && routerKey
        ? getRouterKeyAttributes(eipId, routerKey)
        : {}

    return {
      id: getNodeId(node.id, nodeLookup),
      eipId: {
        ...eipId,
        namespace: EIP_NAMESPACE_TO_XML_PREFIX[namespace] ?? namespace,
      },
      description: state.eipConfigs[node.id]?.description,
      role: eipComponent.role,
      connectionType: eipComponent.connectionType,
      attributes: {
        ...state.eipConfigs[node.id]?.attributes,
        ...routerKeyAttrs.attributes,
        ...routerAttrMap.get(node.id),
      },
      children: buildChildTree(node.id, state.eipConfigs).concat(
        routerKeyAttrs.child ?? [],
        routerChildMap.get(node.id) ?? []
      ),
    }
  })

  return { nodes, edges }
}

const getNodeId = (nodeId: string, nodeLookup: Map<string, CustomNode>) => {
  const node = nodeLookup.get(nodeId)
  if (!node) {
    console.error(`Could not find node with id: ${nodeId}`)
    return nodeId
  }

  switch (node.type) {
    case CustomNodeType.EipNode: {
      return node.data.label ? node.data.label : node.id
    }
    case CustomNodeType.FollowerNode: {
      const leaderEipId = getEipId(node.data.leaderId)
      const descriptor = leaderEipId && describeFollower(leaderEipId)
      if (!descriptor) {
        return node.id
      }
      const leaderNode = nodeLookup.get(node.data.leaderId) as EipFlowNode
      return descriptor.generateLabel(
        leaderNode?.data.label ?? node.data.leaderId
      )
    }
  }
}

const addHiddenFollowerEdges = (nodes: CustomNode[], edges: CustomEdge[]) => {
  const combinedEdges = [...edges]
  nodes.forEach((node) => {
    if (isFollowerNode(node)) {
      const followerDesc = describeFollowerFromId(node.data.leaderId)
      if (followerDesc?.hiddenEdges) {
        const edges = followerDesc.hiddenEdges(
          node.data.leaderId,
          node.id
        ) as CustomEdge[]
        combinedEdges.push(...edges)
      }
    }
  })

  return combinedEdges
}

const buildChildTree = (
  rootId: string,
  eipConfigs: AppStore["eipConfigs"]
): EipChildNode[] => {
  const nodes: Record<string, EipChildNode> = {}

  for (const child of childrenBreadthTraversal(rootId)) {
    const currNode = childConfigToNode(eipConfigs[child.id])
    nodes[child.id] = currNode

    if (child.parentId !== null) {
      const parentNode = nodes[child.parentId]
      parentNode.children
        ? parentNode.children.push(currNode)
        : (parentNode.children = [currNode])
    }
  }

  return nodes[rootId].children ?? []
}

const childConfigToNode = (config: EipConfig): EipChildNode => ({
  name: config.eipId.name,
  attributes: config.attributes,
})

const createNodeLookupMap = (nodes: CustomNode[]) => {
  const map = new Map<string, CustomNode>()
  nodes.forEach((node) => map.set(node.id, node))
  return map
}

// TODO: consider extracting the ChannelMapping and RouterKey logic to the XML translation backend
const addRouterChannelMapping = (
  channelId: string,
  edge: DynamicEdge,
  routerChildMap: Map<string, EipChildNode[]>,
  routerAttributeMap: Map<string, Attributes>
) => {
  const mapping = edge.data?.mapping
  if (!mapping) {
    return
  }

  if (mapping.isDefaultMapping) {
    routerAttributeMap.set(edge.source, {
      [DEFAULT_OUTPUT_CHANNEL_NAME]: channelId,
    })
  } else {
    const child: EipChildNode = {
      name: mapping.mapperName,
      attributes: {
        [CHANNEL_ATTR_NAME]: channelId,
      },
    }
    if (mapping.matcherValue) {
      child.attributes![mapping.matcher.name] = mapping.matcherValue
    }
    const curr = routerChildMap.get(edge.source) ?? []
    routerChildMap.set(edge.source, curr.concat(child))
  }
}

const getRouterKeyAttributes = (eipId: EipId, routerKey: RouterKey) => {
  const routerKeyDef = lookupContentBasedRouterKeys(eipId)
  if (!routerKeyDef) {
    return {}
  }
  switch (routerKeyDef.type) {
    case "attribute": {
      return { attributes: filterEmptyAttrs(routerKey.attributes ?? {}) }
    }
    case "child": {
      return {
        child: {
          name: routerKey.name,
          attributes: filterEmptyAttrs(routerKey.attributes ?? {}),
        },
      }
    }
  }
}

const filterEmptyAttrs = (attrs: Attributes) =>
  Object.keys(attrs).reduce((acc, key) => {
    const val = attrs[key]
    if (val) {
      acc[key] = val
    }
    return acc
  }, {} as Attributes)
