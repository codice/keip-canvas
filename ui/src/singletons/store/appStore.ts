import isDeepEqual from "fast-deep-equal"
import { temporal } from "zundo"
import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"
import debounce from "../../utils/debounce"

import { AppStore } from "./api"

export const EXPORTED_FLOW_VERSION = "1.2"

// If app becomes too slow, might need to switch to async persistent storage.
export const useAppStore = create<AppStore>()(
  persist(
    temporal(
      (_) => ({
        nodes: [],
        edges: [],
        eipConfigs: {},
        selectedChildNode: null,
        layout: {
          orientation: "horizontal",
          density: "comfortable",
        },
        customEntities: {},
      }),
      {
        limit: 50,
        partialize: (state) => {
          const newNodes = state.nodes.map((node) => {
            const n = { ...node }
            const { selected, draggable, dragging, ...rest } = n
            return rest
          })

          const { eipConfigs, edges, layout } = state
          return { eipConfigs, edges, layout, nodes: newNodes }
        },

        equality: (pastState, currentState) =>
          isDeepEqual(pastState, currentState),

        handleSet: (handleSet) =>
          debounce<typeof handleSet>((state) => handleSet(state), 1000, true),
      }
    ),
    {
      name: "eipFlow",
      version: Number(EXPORTED_FLOW_VERSION.split(".")[0]),
      storage: createJSONStorage(() => localStorage),
    }
  )
)
