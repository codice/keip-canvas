import { useShallow } from "zustand/react/shallow"
import { isDynamicEdge } from "../../api/flow"
import { AppStore, SerializedFlow } from "./api"
import { EXPORTED_FLOW_VERSION, useAppStore } from "./appStore"

export const useNodeCount = () => useAppStore((state) => state.nodes.length)

export const useGetNodes = () => useAppStore((state) => state.nodes)

export const useGetLayout = () => useAppStore((state) => state.layout)

export const useFlowStore = () =>
  useAppStore(
    useShallow((state: AppStore) => ({
      nodes: state.nodes,
      edges: state.edges,
    }))
  )

export const useUndoRedo = () =>
  useAppStore(() => ({
    undo: useAppStore.temporal.getState().undo,
    redo: useAppStore.temporal.getState().redo,
  }))

export const useSerializedFlow = () =>
  useAppStore((state) => {
    const flow: SerializedFlow = {
      nodes: state.nodes,
      edges: state.edges,
      eipConfigs: state.eipConfigs,
      customEntities: state.customEntities,
      version: EXPORTED_FLOW_VERSION,
    }
    return JSON.stringify(flow)
  })

export const useGetNodeDescription = (id: string) =>
  useAppStore((state) => state.eipConfigs[id]?.description)

export const useGetEipAttribute = (id: string, attrName: string) =>
  useAppStore((state) => state.eipConfigs[id]?.attributes[attrName])

export const useGetEnabledChildren = (id: string) =>
  useAppStore(
    useShallow((state) =>
      state.eipConfigs[id] ? state.eipConfigs[id].children : []
    )
  )

export const useGetSelectedChildNode = () =>
  useAppStore((state) => state.selectedChildNode)

export const useGetContentRouterKey = (nodeId: string) =>
  useAppStore(useShallow((state) => state.eipConfigs[nodeId]?.routerKey))

export const useGetRouterDefaultEdgeMapping = (routerId: string) =>
  useAppStore((state) =>
    state.edges.find(
      (edge) =>
        edge.source === routerId &&
        isDynamicEdge(edge) &&
        edge.data?.mapping.isDefaultMapping
    )
  )

export const useGetCustomEntityIds = () =>
  useAppStore(useShallow((state) => Object.keys(state.customEntities)))
