import isDeepEqual from "fast-deep-equal"
import { useStoreWithEqualityFn } from "zustand/traditional"
import {
  CustomNode,
  DynamicEdge,
  EipNodeData,
  isDynamicEdge,
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

  const edges: FlowEdge[] = state.edges.map((edge) => {
    const source = nodeLookup.get(edge.source)?.label || edge.source
    const target = nodeLookup.get(edge.target)?.label || edge.target
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
      id: node.data.label ? node.data.label : node.id,
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
  const map = new Map<string, EipNodeData>()
  nodes.forEach((node) => map.set(node.id, node.data))
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
