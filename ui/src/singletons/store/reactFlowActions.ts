import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  Connection,
  EdgeChange,
  NodeChange,
  NodeRemoveChange,
} from "@xyflow/react"
import { CustomEdge, CustomNode, DYNAMIC_EDGE_TYPE } from "../../api/flow"
import { EipComponent } from "../../api/generated/eipComponentDef"
import {
  CHANNEL_ATTR_NAME,
  DYNAMIC_ROUTING_CHILDREN,
  lookupEipComponent,
} from "../eipDefinitions"
import { AppStore } from "./api"
import { useAppStore } from "./appStore"
import { childrenDepthTraversal, getEipId } from "./storeViews"

export const onNodesChange = (changes: NodeChange<CustomNode>[]) =>
  useAppStore.setState((state) => {
    const updates: Partial<AppStore> = {
      nodes: applyNodeChanges(changes, state.nodes),
    }

    const updatedEipConfigs = removeDeletedNodeConfigs(state, changes)
    if (updatedEipConfigs) {
      updates.eipConfigs = updatedEipConfigs
    }

    return updates
  })

export const onEdgesChange = (changes: EdgeChange<CustomEdge>[]) =>
  useAppStore.setState((state) => ({
    edges: applyEdgeChanges(changes, state.edges),
  }))

export const onConnect = (connection: Connection) =>
  useAppStore.setState((state) => {
    const sourceNode = state.nodes.find((n) => n.id === connection.source)
    const sourceEipId = sourceNode && getEipId(sourceNode.id)
    const sourceComponent = sourceEipId && lookupEipComponent(sourceEipId)
    const edge =
      sourceComponent?.connectionType === "content_based_router"
        ? createDynamicRoutingEdge(connection, sourceComponent)
        : connection
    return {
      edges: addEdge(edge, state.edges),
    }
  })

const removeDeletedNodeConfigs = (state: AppStore, changes: NodeChange[]) => {
  const deletes: NodeRemoveChange[] = changes.filter((c) => c.type === "remove")

  if (deletes.length === 0) {
    return null
  }

  const updatedConfigs = { ...state.eipConfigs }
  deletes.forEach((c) => removeNestedConfigs(c.id, updatedConfigs))
  return updatedConfigs
}

const removeNestedConfigs = (root: string, configs: AppStore["eipConfigs"]) => {
  for (const child of childrenDepthTraversal(root)) {
    delete configs[child.id]
  }
}

// TODO: Refactor
const createDynamicRoutingEdge = (
  connection: Connection,
  sourceComponent: EipComponent
) => {
  const channelMapper = sourceComponent?.childGroup?.children.find((child) =>
    DYNAMIC_ROUTING_CHILDREN.has(child.name)
  )
  if (!channelMapper) {
    throw new Error(
      `source component (${sourceComponent.name}) does not have a recognized channel mapping child`
    )
  }

  const matcher = channelMapper.attributes?.find(
    (attr) => attr.name !== CHANNEL_ATTR_NAME
  )
  if (!matcher) {
    throw new Error(
      `Channel mapping component (${channelMapper.name}) does not have a recognized value matcher attribute`
    )
  }

  // TODO: Avoid storing matcher attribute descriptions in the store.
  return {
    ...connection,
    type: DYNAMIC_EDGE_TYPE,
    data: {
      mapping: {
        mapperName: channelMapper.name,
        matcher,
      },
    },
    animated: true,
  }
}
