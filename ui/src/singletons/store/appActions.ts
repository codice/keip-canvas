import { Edge, XYPosition } from "@xyflow/react"
import { produce } from "immer"
import {
  ChannelMapping,
  CustomNode,
  CustomNodeType,
  DYNAMIC_EDGE_TYPE,
  EipFlowNode,
  FollowerNode,
  isDynamicEdge,
  isEipNode,
  Layout,
} from "../../api/flow"
import { AttributeType } from "../../api/generated/eipComponentDef"
import { Attributes, EipId } from "../../api/generated/eipFlow"
import { newFlowLayout } from "../../components/layout/layouting"
import { generateChildId, generateNodeId } from "../../utils/nodeIdGenerator"
import { describeFollower } from "../followerNodeDefs"
import { AppStore, EipConfig, SerializedFlow } from "./api"
import { useAppStore } from "./appStore"
import { childrenDepthTraversal } from "./storeViews"

export const createDroppedNode = (eipId: EipId, position: XYPosition) =>
  useAppStore.setState((state) => {
    const nodeDescriptors = generateNodes(eipId, position)

    const nodeConfigs = nodeDescriptors.reduce(
      (acc, desc) => {
        acc[desc.node.id] = {
          attributes: {},
          children: [],
          eipId: desc.eipId,
        }
        return acc
      },
      {} as AppStore["eipConfigs"]
    )

    return {
      nodes: [...state.nodes, ...nodeDescriptors.map((r) => r.node)],
      eipConfigs: {
        ...state.eipConfigs,
        ...nodeConfigs,
      },
    }
  })

export const updateNodeLabel = (id: string, label: string) => {
  let error: Error | undefined
  useAppStore.setState((state) => {
    const updatedNodes = state.nodes.map((node) => {
      if (node.id === id && isEipNode(node)) {
        return { ...node, data: { ...node.data, label } }
      } else {
        if ("label" in node.data && node.data.label === label) {
          error = new Error("Node labels must be unique")
        }
        return node
      }
    })
    return { nodes: error ? state.nodes : updatedNodes }
  })
  return error
}

export const updateNodeDescription = (id: string, description: string) =>
  useAppStore.setState(
    produce((draft: AppStore) => {
      draft.eipConfigs[id].description = description
    })
  )

export const updateEipAttribute = (
  id: string,
  attrName: string,
  value: AttributeType
) =>
  useAppStore.setState(
    produce((draft: AppStore) => {
      draft.eipConfigs[id].attributes[attrName] = value
    })
  )

export const deleteEipAttribute = (id: string, attrName: string) =>
  useAppStore.setState(
    produce((draft: AppStore) => {
      delete draft.eipConfigs[id].attributes[attrName]
    })
  )

// TODO: Ensure no more than one default mapping edge can be set
export const updateDynamicEdgeMapping = (
  edgeId: string,
  mapping: Partial<ChannelMapping>
) =>
  useAppStore.setState((state) => ({
    edges: state.edges.map((edge) => {
      if (edge.id === edgeId) {
        const dynamic = validateDynamicEdgeType(edge)
        return {
          ...dynamic,
          data: {
            ...dynamic.data,
            mapping: { ...dynamic.data!.mapping, ...mapping },
          },
        }
      }
      return edge
    }),
  }))

export const updateContentRouterKey = (
  nodeId: string,
  keyId: EipId,
  attrName: string,
  value: AttributeType
) =>
  useAppStore.setState(
    produce((draft: AppStore) => {
      const config = draft.eipConfigs[nodeId]
      config.routerKey ??= { eipId: keyId }
      config.routerKey.eipId = keyId

      const routerKey = config.routerKey
      routerKey.attributes ??= {}
      routerKey.attributes[attrName] = value
    })
  )

export const enableChild = (parentId: string, childEipId: EipId) =>
  useAppStore.setState((state) => {
    const childConfig: EipConfig = {
      eipId: childEipId,
      attributes: {},
      children: [],
    }

    const childId = generateChildId()

    return produce(state, (draft: AppStore) => {
      draft.eipConfigs[parentId].children.push(childId)
      draft.eipConfigs[childId] = childConfig
    })
  })

export const disableChild = (parentId: string, childId: string) =>
  useAppStore.setState((state) => {
    const parentConfig = state.eipConfigs[parentId]
    const idx = parentConfig.children.findIndex((id) => id === childId)

    if (idx === -1) {
      throw new Error(
        `Node (id: ${parentId}) did not have child with id: ${childId}`
      )
    }

    // remove target child from parent's children list
    const updatedConfigs = {
      ...state.eipConfigs,
      [parentId]: {
        ...parentConfig,
        children: parentConfig.children.filter((c) => c !== childId),
      },
    }

    // delete target child and its nested children
    for (const child of childrenDepthTraversal(childId)) {
      delete updatedConfigs[child.id]
    }

    return { eipConfigs: updatedConfigs }
  })

export const reorderEnabledChildren = (parentId: string, children: string[]) =>
  useAppStore.setState((state) => {
    if (
      [...children].sort().toString() !==
      [...state.eipConfigs[parentId].children].sort().toString()
    ) {
      throw new Error(
        `Cannot use this method to add/remove child ids, only reordering is allowed
        current list: ${JSON.stringify(state.eipConfigs[parentId].children)}
        requested list: ${JSON.stringify(children)}
        `
      )
    }

    return produce(state, (draft: AppStore) => {
      draft.eipConfigs[parentId].children = children
    })
  })

export const updateSelectedChildNode = (childIdPath: string[]) =>
  useAppStore.setState(() => ({ selectedChildNode: childIdPath }))

export const clearSelectedChildNode = () =>
  useAppStore.setState(() => ({ selectedChildNode: null }))

export const clearFlow = () =>
  useAppStore.setState(() => ({
    nodes: [],
    edges: [],
    eipConfigs: {},
    selectedChildNode: null,
  }))

export const clearDiagramSelections = () =>
  useAppStore.setState((state) => ({
    nodes: state.nodes.map((node) => ({ ...node, selected: false })),
    edges: state.edges.map((edge) => ({ ...edge, selected: false })),
  }))

export const switchNodeSelection = (id: string) =>
  useAppStore.setState((state) => ({
    nodes: state.nodes.map((node) => {
      if (node.id === id) {
        return { ...node, selected: true }
      }
      return { ...node, selected: false }
    }),
  }))

export const importFlowFromJson = (json: string) => {
  const flow = JSON.parse(json) as SerializedFlow
  importFlowFromObject(flow)
}

export const importFlowFromObject = (flow: SerializedFlow) => {
  useAppStore.setState(() => {
    if (!isStoreType(flow)) {
      throw new Error("Failed to import an EIP flow JSON. Malformed input")
    }

    let edges = flow.edges
    let eipConfigs = flow.eipConfigs

    if (!flow.eipConfigs && !flow.version) {
      console.warn(
        "Attempting to import a deprecated EIP Flow format. Attribute configurations will not be preserved."
      )
      return importDeprecatedEipConfigFlow(flow)
    } else if (flow.version === "1.1") {
      console.warn(
        "Attempting to import a deprecated EIP Flow format. Some features may not work correctly."
      )
      const updates = convertDeprecatedRouterKey(eipConfigs, edges)
      edges = updates.edges
      eipConfigs = updates.eipConfigs
    }

    eipConfigs = convertDeprecatedNamespaces(eipConfigs)

    return {
      nodes: flow.nodes,
      edges,
      eipConfigs,
      customEntities: flow.customEntities ?? {},
    }
  })
}

export const updateLayoutOrientation = (orientation: Layout["orientation"]) =>
  useAppStore.setState((state) => {
    const newLayout: Layout = {
      ...state.layout,
      orientation: orientation,
    }
    const nodes = newFlowLayout(state.nodes, state.edges, newLayout)
    return {
      nodes: nodes,
      layout: newLayout,
    }
  })

export const toggleLayoutDensity = () =>
  useAppStore.setState((state) => {
    const newDensity =
      state.layout.density === "compact" ? "comfortable" : "compact"
    const newLayout: Layout = {
      ...state.layout,
      density: newDensity,
    }
    const nodes = newFlowLayout(state.nodes, state.edges, newLayout)
    return {
      nodes: nodes,
      layout: newLayout,
    }
  })

type EntityUpdateResult =
  | { success: true }
  | { success: false; idError?: string; contentError?: string }

export const updateCustomEntity = (
  oldId: string | null,
  newId: string,
  content: string
): EntityUpdateResult => {
  const { customEntities } = useAppStore.getState()

  if (!newId) {
    return { success: false, idError: "An Entity ID is required" }
  }

  const isInPlaceUpdate = oldId === newId
  const isDuplicateId = newId in customEntities

  if (!isInPlaceUpdate && isDuplicateId) {
    return { success: false, idError: "Entity ID must be unique" }
  }

  if (!isWellFormedXML(content)) {
    return {
      success: false,
      contentError: "Content should be a valid XML snippet",
    }
  }

  useAppStore.setState((state) => {
    const updatedEntities = {
      ...state.customEntities,
      [newId]: content,
    }

    if (!isInPlaceUpdate && oldId) {
      delete updatedEntities[oldId]
    }

    return {
      customEntities: updatedEntities,
    }
  })

  return { success: true }
}

export const removeCustomEntity = (entityId: string) =>
  useAppStore.setState((state) => {
    const entities = { ...state.customEntities }
    delete entities[entityId]
    return {
      customEntities: entities,
    }
  })

export const clearAllCustomEntities = () =>
  useAppStore.setState(() => {
    return {
      customEntities: {},
    }
  })

interface NodeDescriptor {
  node: CustomNode
  eipId: EipId
}

const POSITION_X_OFFSET = 200
const POSITION_Y_OFFSET = 0

const generateNodes = (
  eipId: EipId,
  position: XYPosition
): NodeDescriptor[] => {
  const node = newEipNode(position)
  const descriptors: NodeDescriptor[] = [{ node, eipId }]

  const followerDescriptor = describeFollower(eipId)
  if (followerDescriptor) {
    const followerOffset = {
      x: position.x + POSITION_X_OFFSET,
      y: position.y + POSITION_Y_OFFSET,
    }

    const followerNode = newFollowerNode(node.id, followerOffset)
    node.data.followerId = followerNode.id
    descriptors.push({ node: followerNode, eipId: followerDescriptor.eipId })
  }

  return descriptors
}

export const newEipNode = (position: XYPosition) => {
  const id = generateNodeId()
  const node: EipFlowNode = {
    id: id,
    type: CustomNodeType.EipNode,
    position: position,
    data: {},
  }
  return node
}

const newFollowerNode = (leaderId: string, position: XYPosition) => {
  const id = generateNodeId()
  const node: FollowerNode = {
    id: id,
    type: CustomNodeType.FollowerNode,
    position: position,
    data: {
      leaderId,
    },
  }
  return node
}

const validateDynamicEdgeType = (edge: Edge) => {
  if (!isDynamicEdge(edge)) {
    throw new Error(
      `The provided edge did not have the expected type: "${edge.type}". Should be "${DYNAMIC_EDGE_TYPE}"`
    )
  }
  return edge
}

const isStoreType = (state: unknown): state is AppStore => {
  const store = state as SerializedFlow & {
    eipNodeConfigs: Record<string, object>
  }

  // Accept deprecated flows for now
  const hasOldConfigKey = store.eipNodeConfigs !== undefined

  return (
    store.nodes !== undefined &&
    store.edges !== undefined &&
    (store.eipConfigs !== undefined || hasOldConfigKey)
  )
}

// Maintain backwards compatibility with older exported formats
const importDeprecatedEipConfigFlow = (
  flow: SerializedFlow
): Partial<AppStore> => {
  const eipConfigs = {} as AppStore["eipConfigs"]
  const nodes = flow.nodes.map((node) => {
    const { eipId: oldEipId, ...rest } = node.data as { eipId: EipId }
    eipConfigs[node.id] = {
      attributes: {},
      children: [],
      eipId: fixDeprecatedEipId(oldEipId),
    }
    return {
      ...node,
      data: rest,
    }
  }) as CustomNode[]

  return {
    nodes,
    edges: flow.edges,
    eipConfigs,
  }
}

// Maintain backwards compatibility with older exported routerKeys
const convertDeprecatedRouterKey = (
  eipConfigs: SerializedFlow["eipConfigs"],
  edges: SerializedFlow["edges"]
) => {
  const updatedConfigs = Object.fromEntries(
    Object.entries(eipConfigs).map(([id, config]) => {
      if (config.routerKey && !config.routerKey.eipId) {
        const oldRouterKey = config.routerKey as unknown as {
          name: string
          attributes?: Attributes
        }

        const { name, attributes } = oldRouterKey

        return [
          id,
          {
            ...config,
            routerKey: {
              attributes,
              eipId: {
                namespace: fixDeprecatedEipId(config.eipId).namespace,
                name,
              },
            },
          },
        ]
      }

      return [id, config]
    })
  )

  const updatedEdges = edges.map((edge) => {
    if (isDynamicEdge(edge) && edge.data?.mapping) {
      const oldMapping = edge.data.mapping as Partial<ChannelMapping> & {
        mapperName: string
      }

      const { mapperName, ...rest } = oldMapping

      return {
        ...edge,
        data: {
          mapping: {
            ...rest,
            mapperId: {
              namespace: fixDeprecatedEipId(eipConfigs[edge.source].eipId)
                .namespace,
              name: mapperName,
            },
          } as ChannelMapping,
        },
      }
    }

    return edge
  })

  return { eipConfigs: updatedConfigs, edges: updatedEdges }
}

const DEPRECATED_EIP_NAMESPACES: Record<string, string> = {
  xml: "int-xml",
  "web-services": "ws",
}

// Maintain backwards compatibility with older exported namespaces
const convertDeprecatedNamespaces = (
  eipConfigs: SerializedFlow["eipConfigs"]
) =>
  Object.fromEntries(
    Object.entries(eipConfigs).map(([id, config]) => {
      const eipId = config.eipId
      if (eipId.namespace in DEPRECATED_EIP_NAMESPACES) {
        const convertedEipId = fixDeprecatedEipId(eipId)

        console.warn(
          `Imported flow has a deprecated namespace '${eipId.namespace}'. Converting the namespace to '${convertedEipId.namespace}'.`
        )

        return [
          id,
          {
            ...config,
            eipId: convertedEipId,
          },
        ]
      }

      return [id, config]
    })
  )

const fixDeprecatedEipId = (eipId: EipId): EipId => ({
  ...eipId,
  namespace: DEPRECATED_EIP_NAMESPACES[eipId.namespace] ?? eipId.namespace,
})

const isWellFormedXML = (content: string) => {
  if (!content) {
    return false
  }

  const xmlDoc = new DOMParser().parseFromString(content, "text/xml")
  const errorNode = xmlDoc.querySelector("parsererror")
  return errorNode === null
}
