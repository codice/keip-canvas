import { act } from "@testing-library/react"
import { beforeEach, describe, expect, test, vi } from "vitest"
import { RouterKey } from "../../api/flow"
import {
  useGetContentRouterKey,
  useGetEipAttribute,
  useGetEnabledChildren,
  useGetNodeDescription,
  useGetRouterDefaultEdgeMapping,
  useSerializedFlow,
} from "./getterHooks"
import { renderAndUnwrapHook, resetMockStore } from "./storeTestingUtils"
import childBasedRouterFlow from "./testdata/store-initializers/childBasedRouterFlow.json"
import standardFlow from "./testdata/store-initializers/standardFlow.json"

vi.mock("zustand")

const STANDARD_INBOUND_ADAPTER = "9KWCqlIyy7"
const STANDARD_ROUTER = "LoiC2CFbLP"
const STANDARD_POLLER_CHILD = "mcyTryMPewJ"

// TODO: Do we really need to use 'act'?
beforeEach(() => {
  act(() => {
    resetMockStore(standardFlow)
  })
})

test("serialized store includes a subset of AppStore", () => {
  const storeJson = renderAndUnwrapHook(useSerializedFlow)
  expect(storeJson).toMatchSnapshot()
})

describe("get node description", () => {
  test.each([
    {
      msg: "node id is present -> value",
      id: STANDARD_INBOUND_ADAPTER,
      result: "message incoming",
    },
    {
      msg: "node id is present but no description -> undefined",
      id: STANDARD_ROUTER,
      result: undefined,
    },
    {
      msg: "unknown node id -> undefined",
      id: "fakeid",
      result: undefined,
    },
  ])("$msg", ({ id, result }) => {
    const description = renderAndUnwrapHook(() => useGetNodeDescription(id))
    expect(description).toEqual(result)
  })
})

describe("get EIP attribute", () => {
  test.each([
    {
      msg: "root node id and attribute are present -> value",
      id: STANDARD_ROUTER,
      attrName: "phase",
      result: "init",
    },
    {
      msg: "child id and attribute are present -> value",
      id: STANDARD_POLLER_CHILD,
      attrName: "fixed-rate",
      result: "2000",
    },
    {
      msg: "node id is present but attribute name is unknown -> undefined",
      id: STANDARD_ROUTER,
      attrName: "fakeattr",
      result: undefined,
    },
    {
      msg: "unknown root node id -> undefined",
      id: "fakeid",
      attrName: "ref",
      result: undefined,
    },
  ])("$msg", ({ id, attrName, result }) => {
    const value = renderAndUnwrapHook(() => useGetEipAttribute(id, attrName))
    expect(value).toEqual(result)
  })
})

describe("get list of children", () => {
  test("node has children -> value", () => {
    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(STANDARD_INBOUND_ADAPTER)
    )
    expect(children).toEqual([STANDARD_POLLER_CHILD])
  })

  test("node does not have children -> empty array", () => {
    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(STANDARD_ROUTER)
    )
    expect(children).toEqual([])
  })

  test("unknown node id -> empty array", () => {
    const children = renderAndUnwrapHook(() => useGetEnabledChildren("fakeid"))
    expect(children).toEqual([])
  })
})

describe("get content router key", () => {
  test("node id is a router -> value", () => {
    const key = renderAndUnwrapHook(() =>
      useGetContentRouterKey(STANDARD_ROUTER)
    )
    const expected: RouterKey = {
      eipId: { namespace: "integration", name: "expression" },
      attributes: { expression: "headers.protocol" },
    }
    expect(key).toEqual(expected)
  })

  test("unknown node id -> value", () => {
    const key = renderAndUnwrapHook(() => useGetContentRouterKey("fakeid"))
    expect(key).toBeUndefined()
  })
})

describe("get content router default edge mapping", () => {
  test("node is a router and has a default edge -> value", () => {
    const edge = renderAndUnwrapHook(() =>
      useGetRouterDefaultEdgeMapping(STANDARD_ROUTER)
    )
    expect(edge?.id).toEqual("reactflow__edge-LoiC2CFbLPoutput-H4-ED4F0XTinput")
  })

  test("node is not a router -> undefined", () => {
    const edge = renderAndUnwrapHook(() =>
      useGetRouterDefaultEdgeMapping(STANDARD_INBOUND_ADAPTER)
    )
    expect(edge).toBeUndefined()
  })

  test("node is a router with no default mapping -> undefined", () => {
    act(() => {
      resetMockStore(childBasedRouterFlow)
    })
    const edge = renderAndUnwrapHook(() =>
      useGetRouterDefaultEdgeMapping("cGUxaVOQ7L")
    )
    expect(edge).toBeUndefined()
  })
})
