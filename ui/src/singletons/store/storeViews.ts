import { CustomEdge, CustomNode, Layout } from "../../api/flow"
import { EipId } from "../../api/generated/eipFlow"
import { useAppStore } from "./appStore"

interface ChildTraversalItem {
  id: string
  parentId: string | null
}

// WARNING: store-based exports defined here provide non-reactive access to the store's state

export const getNodesView = (): readonly CustomNode[] =>
  useAppStore.getState().nodes

export const getEdgesView = (): readonly CustomEdge[] =>
  useAppStore.getState().edges

export const getLayoutView = (): Readonly<Layout> =>
  useAppStore.getState().layout

export const getEnabledChildrenView = (
  nodeId: string
): readonly string[] | undefined =>
  useAppStore.getState().eipConfigs[nodeId]?.children

export const getEipId = (nodeId: string): Readonly<EipId> | undefined =>
  useAppStore.getState().eipConfigs[nodeId]?.eipId

export const getSelectedChildNode = (): readonly string[] | null =>
  useAppStore.getState().selectedChildNode

export const getCustomEntityContent = (
  entityId: string
): Readonly<string> | undefined =>
  useAppStore.getState().customEntities[entityId]

export const childrenBreadthTraversal = function* (
  rootId: string
): Generator<ChildTraversalItem> {
  const eipConfigs = useAppStore.getState().eipConfigs

  const queue: ChildTraversalItem[] = [{ id: rootId, parentId: null }]

  while (queue.length > 0) {
    const curr = queue.shift()!

    const config = eipConfigs[curr.id]
    if (config) {
      config.children &&
        config.children.forEach((child) =>
          queue.push({ id: child, parentId: curr.id })
        )
    }

    yield curr
  }
}

export const childrenDepthTraversal = function* (
  rootId: string
): Generator<ChildTraversalItem> {
  const eipConfigs = useAppStore.getState().eipConfigs

  const stack: ChildTraversalItem[] = [{ id: rootId, parentId: null }]

  while (stack.length > 0) {
    const curr = stack.pop()!

    const config = eipConfigs[curr.id]
    if (config) {
      config.children &&
        config.children.forEach((child) =>
          stack.push({ id: child, parentId: curr.id })
        )
    }

    yield curr
  }
}
