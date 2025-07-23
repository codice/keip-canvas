import { act } from "@testing-library/react"
import { Connection, NodeRemoveChange } from "@xyflow/react"
import { beforeEach, describe, expect, test, vi } from "vitest"
import { DYNAMIC_EDGE_TYPE, DynamicEdgeData } from "../../api/flow"
import { useGetEnabledChildren } from "./getterHooks"
import { onConnect, onNodesChange } from "./reactFlowActions"
import { renderAndUnwrapHook, resetMockStore } from "./storeTestingUtils"
import { getEdgesView, getEipId, getNodesView } from "./storeViews"
import disconnectedFlow from "./testdata/store-initializers/disconnectedFlow.json"
import standardFlow from "./testdata/store-initializers/standardFlow.json"

vi.mock("zustand")

const STANDARD_FILE_ADAPTER_ID = "MrMneIdthg"

beforeEach(() => {
  act(() => {
    resetMockStore(standardFlow)
  })
})

describe("onNodesChange", () => {
  test("top-level config and all child configs are removed when node is deleted", () => {
    // assert configs exist before removing node
    expect(getEipId(STANDARD_FILE_ADAPTER_ID)).not.toBeUndefined()
    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(STANDARD_FILE_ADAPTER_ID)
    )
    expect(children).toHaveLength(1)
    const childId = children[0]
    expect(getEipId(childId)).not.toBeUndefined()

    const change: NodeRemoveChange = {
      id: STANDARD_FILE_ADAPTER_ID,
      type: "remove",
    }
    act(() => onNodesChange([change]))

    const checkDeleted = getNodesView().find(
      (n) => n.id === STANDARD_FILE_ADAPTER_ID
    )
    expect(checkDeleted).toBeUndefined()

    expect(getEipId(STANDARD_FILE_ADAPTER_ID)).toBeUndefined()
    expect(getEipId(childId)).toBeUndefined()
  })
})

describe("onConnect", () => {
  beforeEach(() => {
    act(() => {
      resetMockStore(disconnectedFlow)
    })
  })

  test("connecting a default (non-router) edge", () => {
    const connection: Connection = {
      source: "yiSAwF9JPY",
      target: "1QUBRFgUcd",
      sourceHandle: null,
      targetHandle: null,
    }

    act(() => onConnect(connection))

    const edges = getEdgesView()
    expect(edges).toHaveLength(1)

    const edge = edges[0]
    expect(edge).toMatchObject({
      source: connection.source,
      target: connection.target,
    })
    expect(edge.type).toBeUndefined()
    expect(edge.animated).toBeUndefined()
    expect(edge.data).toBeUndefined()
  })

  test("connecting a dynamic router edge", () => {
    const connection: Connection = {
      source: "1QUBRFgUcd",
      target: "XMypRiNV-I",
      sourceHandle: null,
      targetHandle: null,
    }

    act(() => onConnect(connection))

    const edges = getEdgesView()
    expect(edges).toHaveLength(1)

    const edge = edges[0]
    expect(edge.source).toEqual(connection.source)
    expect(edge.target).toEqual(connection.target)
    expect(edge.type).toEqual(DYNAMIC_EDGE_TYPE)
    expect(edge.animated).toBe(true)

    const data = edge.data as DynamicEdgeData
    expect(data.mapping.mapperName).toEqual("mapping")
    expect(data.mapping.matcher.name).toEqual("value")
  })
})
