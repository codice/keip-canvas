import {
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  Connection,
  EdgeChange,
  NodeChange,
  NodeRemoveChange,
} from "@xyflow/react"
import {
  CustomEdge,
  CustomNode,
  CustomNodeType,
  DYNAMIC_EDGE_TYPE,
} from "../../api/flow"
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
    const removals: NodeRemoveChange[] = changes.filter(
      (c) => c.type === "remove"
    )

    const followerRemovals = resolveFollowerRemovals(state.nodes, removals)

    const allChanges = [...changes, ...followerRemovals]

    const updates: Partial<AppStore> = {
      nodes: applyNodeChanges(allChanges, state.nodes),
    }

    const updatedEipConfigs = removeDeletedNodeConfigs(state, [
      ...removals,
      ...followerRemovals,
    ])

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

/*
Assumption 1: Deleting a leader node deletes its follower
Assumption 2: Deleting a follower node deletes its leader

Returns a list of additional NodeRemoveChanges that need to be applied
*/
const resolveFollowerRemovals = (
  nodes: CustomNode[],
  changes: NodeRemoveChange[]
) => {
  const nodeLookup = createNodeLookupMap(nodes)
  const newRemovalIds = new Set<string>()

  changes.forEach((change) => {
    const node = nodeLookup.get(change.id)
    if (!node) {
      throw new Error(
        `attempting to delete a non-existent node (id: ${change.id})`
      )
    }

    switch (node.type) {
      case CustomNodeType.EipNode: {
        node.data.followerId && newRemovalIds.add(node.data.followerId)
        break
      }
      case CustomNodeType.FollowerNode: {
        node.data.leaderId && newRemovalIds.add(node.data.leaderId)
        break
      }
    }
  })

  return [...newRemovalIds].map(
    (id) =>
      ({
        type: "remove",
        id,
      }) as NodeRemoveChange
  )
}

const createNodeLookupMap = (nodes: CustomNode[]) => {
  const map = new Map<string, CustomNode>()
  nodes.forEach((node) => map.set(node.id, node))
  return map
}

const removeDeletedNodeConfigs = (
  state: AppStore,
  removals: NodeRemoveChange[]
) => {
  if (removals.length === 0) {
    return null
  }

  const updatedConfigs = { ...state.eipConfigs }
  removals.forEach((c) => removeNestedConfigs(c.id, updatedConfigs))
  return updatedConfigs
}

const removeNestedConfigs = (root: string, configs: AppStore["eipConfigs"]) => {
  for (const child of childrenDepthTraversal(root)) {
    delete configs[child.id]
  }
}

// TODO: Refactor
export const createDynamicRoutingEdge = (
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
