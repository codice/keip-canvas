import { act } from "@testing-library/react"
import { Position } from "@xyflow/react"
import isDeepEqual from "fast-deep-equal"
import { beforeEach, describe, expect, test, vi } from "vitest"
import {
  ChannelMapping,
  DynamicEdgeData,
  EIP_NODE_TYPE,
  Layout,
  RouterKey,
} from "../../api/flow"
import {
  useGetContentRouterKey,
  useGetEipAttribute,
  useGetEnabledChildren,
  useGetNodeDescription,
  useGetSelectedChildNode,
} from "./getterHooks"
import { renderAndUnwrapHook, resetMockStore } from "./storeTestingUtils"
import nestedChildFlow from "./testdata/store-initializers/nestedChildFlow.json"
import selectedNodeFlow from "./testdata/store-initializers/singleSelectedNodeFlow.json"
import standardFlow from "./testdata/store-initializers/standardFlow.json"
import unspecifiedRouterFlow from "./testdata/store-initializers/unspecifiedRouterFlow.json"
import verticalFlow from "./testdata/store-initializers/verticalFlow.json"

import { EipId } from "../../api/generated/eipFlow"
import {
  clearDiagramSelections,
  clearFlow,
  createDroppedNode,
  deleteEipAttribute,
  disableChild,
  enableChild,
  importFlowFromJson,
  reorderEnabledChildren,
  toggleLayoutDensity,
  updateContentRouterKey,
  updateDynamicEdgeMapping,
  updateEipAttribute,
  updateLayoutOrientation,
  updateNodeDescription,
  updateNodeLabel,
  updateSelectedChildNode,
} from "./appActions"
import {
  getEdgesView,
  getEipId,
  getLayoutView,
  getNodesView,
} from "./storeViews"
import deprecatedExportedFlow from "./testdata/exported-diagrams/deprecatedFlow.json?raw"
import validExportedFlow from "./testdata/exported-diagrams/validFlow.json?raw"

vi.mock("zustand")

const STANDARD_INBOUND_ADAPTER = "9KWCqlIyy7"
const STANDARD_ROUTER = "LoiC2CFbLP"
const STANDARD_POLLER_CHILD = "mcyTryMPewJ"
const STANDARD_TRANSACTIONAL_CHILD = "V1ls9ri4szs"
const NESTED_CHILD_PARENT_ID = "FL5Tssm8tV"

beforeEach(() => {
  act(() => {
    resetMockStore(standardFlow)
  })
})

const getNode = (id: string) => {
  const nodes = getNodesView()
  const target = nodes.find((n) => n.id === id)
  if (!target) {
    throw new Error(`Could not find node with id: ${id}`)
  }
  return target
}

const getNodePositions = () =>
  getNodesView().map((node) => ({
    [node.id]: {
      position: node.position,
    },
  }))

describe("create dropped node", () => {
  test.each([
    {
      msg: "create with horizontal layout",
      storeState: standardFlow,
      orientation: "horizontal",
      handlePosition: {
        targetPosition: Position.Left,
        sourcePosition: Position.Right,
      },
    },
    {
      msg: "create with vertical layout",
      storeState: verticalFlow,
      orientation: "vertical",
      handlePosition: {
        targetPosition: Position.Top,
        sourcePosition: Position.Bottom,
      },
    },
  ])("$msg", ({ storeState, orientation, handlePosition }) => {
    // Initialize store
    act(() => {
      resetMockStore(storeState)
    })

    // Assert expected layout
    const layout = getLayoutView()
    expect(layout.orientation).toEqual(orientation)

    const eipId = { namespace: "test", name: "transformer" }
    const position = { x: 5, y: 10 }
    act(() => createDroppedNode(eipId, position))

    const nodes = getNodesView()
    const node = nodes.find((n) => isDeepEqual(getEipId(n.id), eipId))!

    expect(node.position).toEqual(position)
    expect(node.id).toBeTruthy()
    expect(node.type).toEqual(EIP_NODE_TYPE)
    expect(node.targetPosition).toEqual(handlePosition.targetPosition)
    expect(node.sourcePosition).toEqual(handlePosition.sourcePosition)
  })
})

describe("update node label", () => {
  test("update node with unique label success", () => {
    const label = "test-label-123"

    act(() => void updateNodeLabel(STANDARD_INBOUND_ADAPTER, label))

    const target = getNode(STANDARD_INBOUND_ADAPTER)
    expect(target.data.label).toEqual(label)
  })

  test("update node with duplicated label -> error", () => {
    let target = getNode(STANDARD_INBOUND_ADAPTER)
    const originalLabel = target.data.label

    const duplicatedLabel = "test-router"

    const result = updateNodeLabel(STANDARD_INBOUND_ADAPTER, duplicatedLabel)
    expect(result instanceof Error).toBeTruthy()

    target = getNode(STANDARD_INBOUND_ADAPTER)
    expect(target.data.label).toEqual(originalLabel)
  })

  test("update non-existent node is a no-op", () => {
    const before = getNodesView()
    act(() => void updateNodeLabel("fakeid", "test-label"))
    const after = getNodesView()
    expect(before).toEqual(after)
  })
})

describe("update node description", () => {
  test("update node description success", () => {
    const description = "testing description update"
    act(() => updateNodeDescription(STANDARD_INBOUND_ADAPTER, description))

    const actual = renderAndUnwrapHook(() =>
      useGetNodeDescription(STANDARD_INBOUND_ADAPTER)
    )
    expect(actual).toEqual(description)
  })

  test("update non-existent node throws error", () => {
    expect(() => updateNodeDescription("fakeid", "test")).toThrowError()
  })
})

describe("updating EIP Attributes", () => {
  test.each([
    {
      msg: "add root node attr",
      nodeId: STANDARD_ROUTER,
      attribute: {
        name: "foo",
        value: "bar",
      },
    },
    {
      msg: "add the first attr for a root node",
      nodeId: STANDARD_INBOUND_ADAPTER,
      attribute: {
        name: "foo",
        value: "bar",
      },
    },
    {
      msg: "add child node attr",
      nodeId: STANDARD_POLLER_CHILD,
      attribute: {
        name: "child-foo",
        value: "child-bar",
      },
    },
    {
      msg: "add the first attr for a child node",
      nodeId: STANDARD_TRANSACTIONAL_CHILD,
      attribute: {
        name: "child-foo",
        value: "child-bar",
      },
    },
  ])("$msg", ({ nodeId, attribute }) => {
    act(() => updateEipAttribute(nodeId, attribute.name, attribute.value))

    const actual = renderAndUnwrapHook(() =>
      useGetEipAttribute(nodeId, attribute.name)
    )
    expect(actual).toEqual(attribute.value)
  })

  test.each([
    {
      msg: "overwrite root node attr",
      nodeId: STANDARD_ROUTER,
      attribute: {
        name: "phase",
        value: "last",
      },
    },
    {
      msg: "overwrite child node attr",
      nodeId: STANDARD_POLLER_CHILD,
      attribute: {
        name: "fixed-rate",
        value: 500,
      },
    },
  ])("$msg", ({ nodeId, attribute }) => {
    // assert attribute actually exists before updating
    let actual = renderAndUnwrapHook(() =>
      useGetEipAttribute(nodeId, attribute.name)
    )
    expect(actual).toBeTruthy()
    expect(actual).not.toEqual(attribute.value)

    act(() => updateEipAttribute(nodeId, attribute.name, attribute.value))

    actual = renderAndUnwrapHook(() =>
      useGetEipAttribute(nodeId, attribute.name)
    )
    expect(actual).toEqual(attribute.value)
  })

  test("update non-existent node throws error", () => {
    const attribute = { name: "foo", value: "bar" }
    expect(() =>
      updateEipAttribute("fakeid", attribute.name, attribute.value)
    ).toThrowError()
  })
})

describe("deleting EIP Attributes", () => {
  test("delete existing node attribute success", () => {
    const nodeId = STANDARD_ROUTER
    const attrName = "phase"

    // assert attribute exists before deleting
    let attribute = renderAndUnwrapHook(() =>
      useGetEipAttribute(nodeId, attrName)
    )
    expect(attribute).toBeTruthy()

    act(() => deleteEipAttribute(nodeId, attrName))

    attribute = renderAndUnwrapHook(() => useGetEipAttribute(nodeId, attrName))
    expect(attribute).toBeUndefined()
  })

  test("delete non-existent node attribute is a no-op", () => {
    const attrName = "fakeattr"

    act(() => deleteEipAttribute(STANDARD_ROUTER, attrName))

    const attribute = renderAndUnwrapHook(() =>
      useGetEipAttribute(STANDARD_ROUTER, attrName)
    )
    expect(attribute).toBeUndefined()
  })
})

describe("mapping a dynamic router edge", () => {
  test("update a dynamic routing edge mapping success", () => {
    const edgeId = "reactflow__edge-LoiC2CFbLPoutput-SV43RVeijQinput"
    const mapping: ChannelMapping = {
      mapperName: "test-mapping",
      matcher: { name: "test-match-attr", type: "string" },
      matcherValue: "abc",
    }

    act(() => updateDynamicEdgeMapping(edgeId, mapping))

    const edge = getEdgesView().find((edge) => edge.id === edgeId)!
    const data = edge.data as DynamicEdgeData
    expect(data.mapping).toEqual(mapping)
  })

  test("update a default edge throws an error", () => {
    const edgeId = "reactflow__edge-9KWCqlIyy7output-LoiC2CFbLPinput"
    const mapping: ChannelMapping = {
      mapperName: "test-mapping",
      matcher: { name: "test-match-attr", type: "string" },
      matcherValue: "abc",
    }

    expect(() => updateDynamicEdgeMapping(edgeId, mapping)).toThrowError()
  })
})

describe("update content router key", () => {
  test("update existing content router key success", () => {
    const attrName = "expression"
    const attrValue = "bar"
    const routerKey: RouterKey = {
      name: "test-key",
      attributes: { [attrName]: attrValue },
    }

    act(() =>
      updateContentRouterKey(
        STANDARD_ROUTER,
        routerKey.name,
        attrName,
        attrValue
      )
    )

    const actual = renderAndUnwrapHook(() =>
      useGetContentRouterKey(STANDARD_ROUTER)
    )
    expect(actual).toEqual(routerKey)
  })

  test("create new content router key success", () => {
    act(() => resetMockStore(unspecifiedRouterFlow))
    const routerId = "ihLSJU8QVc"

    const attrName = "expression"
    const attrValue = "bar"
    const routerKey: RouterKey = {
      name: "test-key",
      attributes: { [attrName]: attrValue },
    }

    act(() =>
      updateContentRouterKey(routerId, routerKey.name, attrName, attrValue)
    )

    const actual = renderAndUnwrapHook(() => useGetContentRouterKey(routerId))
    expect(actual).toEqual(routerKey)
  })

  test("add additional router key attribute success", () => {
    const initialKey = renderAndUnwrapHook(() =>
      useGetContentRouterKey(STANDARD_ROUTER)
    )

    const attrName = "foo"
    const attrValue = "bar"
    const routerKey: RouterKey = {
      name: "test-key",
      attributes: { ...initialKey?.attributes, [attrName]: attrValue },
    }

    act(() =>
      updateContentRouterKey(
        STANDARD_ROUTER,
        routerKey.name,
        attrName,
        attrValue
      )
    )

    const actual = renderAndUnwrapHook(() =>
      useGetContentRouterKey(STANDARD_ROUTER)
    )
    expect(actual).toEqual(routerKey)
  })

  // TODO: unskip this test once action is fixed
  test.skip("update non-router node throws error", () => {
    expect(() =>
      updateContentRouterKey(STANDARD_INBOUND_ADAPTER, "test-key", "foo", "bar")
    ).toThrowError()
  })

  test("update non-existent node throws error", () => {
    expect(() =>
      updateContentRouterKey("fakeid", "test-key", "foo", "bar")
    ).toThrowError()
  })
})

test("enable child success", () => {
  const child1: EipId = { namespace: "test", name: "c1" }
  const child2: EipId = { namespace: "other", name: "c2" }

  act(() => enableChild(STANDARD_ROUTER, child1))
  act(() => enableChild(STANDARD_ROUTER, child2))

  const children = renderAndUnwrapHook(() =>
    useGetEnabledChildren(STANDARD_ROUTER)
  )

  expect(children).toHaveLength(2)
  expect(getEipId(children[0])).toEqual(child1)
  expect(getEipId(children[1])).toEqual(child2)
})

describe("disable child", () => {
  test("disable child success", () => {
    act(() => disableChild(STANDARD_INBOUND_ADAPTER, STANDARD_POLLER_CHILD))

    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(STANDARD_INBOUND_ADAPTER)
    )

    expect(children).toHaveLength(0)
    expect(getEipId(STANDARD_POLLER_CHILD)).toBeUndefined()
  })

  test("disable child deletes all nested children", () => {
    act(() => resetMockStore(nestedChildFlow))

    const initChildren = renderAndUnwrapHook(() =>
      useGetEnabledChildren(NESTED_CHILD_PARENT_ID)
    )
    expect(initChildren).toHaveLength(3)

    const topChildId = "43FAdk5SdBR"
    act(() => disableChild(NESTED_CHILD_PARENT_ID, topChildId))

    const updatedChildren = renderAndUnwrapHook(() =>
      useGetEnabledChildren(NESTED_CHILD_PARENT_ID)
    )
    expect(updatedChildren).toHaveLength(initChildren.length - 1)

    expect(getEipId(topChildId)).toBeUndefined()
    expect(getEipId("9OymVfY4n4p")).toBeUndefined()
    expect(getEipId("gCL0YkrFGgW")).toBeUndefined()
  })

  test("disable child unknown child id -> error", () => {
    expect(() =>
      disableChild(STANDARD_INBOUND_ADAPTER, "fakeid")
    ).toThrowError()

    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(STANDARD_INBOUND_ADAPTER)
    )

    expect(children).toHaveLength(1)
  })

  test("disable child unknown parent id -> error", () => {
    expect(() => disableChild("fakeid", STANDARD_POLLER_CHILD)).toThrowError()

    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(STANDARD_INBOUND_ADAPTER)
    )

    expect(children).toHaveLength(1)
  })
})

describe("reorder enabled child list", () => {
  beforeEach(() => {
    act(() => {
      resetMockStore(nestedChildFlow)
    })
  })

  test("reorder enabled children success", () => {
    const initial = renderAndUnwrapHook(() =>
      useGetEnabledChildren(NESTED_CHILD_PARENT_ID)
    )

    const updated = [...initial]

    // swap first and third elements
    const temp = updated[2]
    updated[2] = updated[0]
    updated[0] = temp

    act(() => reorderEnabledChildren(NESTED_CHILD_PARENT_ID, updated))

    const children = renderAndUnwrapHook(() =>
      useGetEnabledChildren(NESTED_CHILD_PARENT_ID)
    )
    expect(children).toEqual(updated)
  })

  test("adding child -> error ", () => {
    const initial = renderAndUnwrapHook(() =>
      useGetEnabledChildren(NESTED_CHILD_PARENT_ID)
    )
    const updated = [...initial, "extra"]
    expect(() =>
      reorderEnabledChildren(NESTED_CHILD_PARENT_ID, updated)
    ).toThrowError()
  })

  test("removing child -> error ", () => {
    const initial = renderAndUnwrapHook(() =>
      useGetEnabledChildren(NESTED_CHILD_PARENT_ID)
    )
    const updated = initial.slice(1)
    expect(() =>
      reorderEnabledChildren(NESTED_CHILD_PARENT_ID, updated)
    ).toThrowError()
  })

  test("changing child list -> error ", () => {
    expect(() =>
      reorderEnabledChildren(NESTED_CHILD_PARENT_ID, ["c1", "c2"])
    ).toThrowError()
  })
})

test("update selected child node success", () => {
  const childPath = ["root", "c1"]

  act(() => updateSelectedChildNode(childPath))

  const actual = renderAndUnwrapHook(useGetSelectedChildNode)
  expect(actual).toEqual(childPath)
})

test("clear flow success", () => {
  act(() => clearFlow())

  const nodes = getNodesView()
  const edges = getEdgesView()
  const selectedChildNode = renderAndUnwrapHook(useGetSelectedChildNode)

  expect(nodes).toHaveLength(0)
  expect(edges).toHaveLength(0)
  expect(selectedChildNode).toBeNull()
})

test("clear top-level node selections from diagram success", () => {
  // Initialize store
  act(() => {
    resetMockStore(selectedNodeFlow)
  })

  // assert a selected node is present before clearing
  let selected = getNodesView().find((n) => n.selected)
  expect(selected).not.toBeUndefined()

  act(() => clearDiagramSelections())

  selected = getNodesView().find((n) => n.selected)
  expect(selected).toBeUndefined()
})

describe("import flow from an exported JSON file", () => {
  test.each([
    {
      msg: "import a valid flow success",
      flow: validExportedFlow,
    },
    {
      msg: "import a deprecated flow partial success",
      flow: deprecatedExportedFlow,
    },
  ])("$msg", ({ flow }) => {
    const initNodes = getNodesView()

    act(() => importFlowFromJson(flow))

    const nodes = getNodesView()

    const updatedState = {
      nodes: nodes,
      edges: getEdgesView(),
      children: nodes.map((node) => ({
        [node.id]: renderAndUnwrapHook(() => useGetEnabledChildren(node.id)),
      })),
      eipIds: nodes.map((node) => getEipId(node.id)),
      layout: getLayoutView(),
    }

    expect(updatedState.nodes).not.toEqual(initNodes)
    expect(updatedState).toMatchSnapshot()
  })

  test.each([{ key: "nodes" }, { key: "edges" }, { key: "eipConfigs" }])(
    "import a malformed flow (missing '$key' field) is a no-op",
    ({ key }) => {
      const initialNodes = getNodesView()
      const initialEdges = getEdgesView()

      // Invalidate the flow JSON
      const flow = JSON.parse(validExportedFlow) as Record<string, object>
      delete flow[key]

      expect(() => importFlowFromJson(JSON.stringify(flow))).toThrowError()
      expect(getNodesView()).toEqual(initialNodes)
      expect(getEdgesView()).toEqual(initialEdges)
    }
  )
})

describe("update layout orientation", () => {
  test.each([
    {
      orientation: "horizontal",
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
    },
    {
      orientation: "vertical",
      sourcePosition: Position.Bottom,
      targetPosition: Position.Top,
    },
  ])(
    "set to $orientation",
    ({ orientation, sourcePosition, targetPosition }) => {
      act(() => updateLayoutOrientation(orientation as Layout["orientation"]))

      expect(getLayoutView().orientation).toEqual(orientation)

      getNodesView().forEach((node) => {
        expect(node.sourcePosition).toEqual(sourcePosition)
        expect(node.targetPosition).toEqual(targetPosition)
      })
    }
  )
})

test("toggle layout density", () => {
  expect(getLayoutView().density).toEqual("comfortable")

  const initialPositions = getNodePositions()

  act(() => toggleLayoutDensity())
  const compactPositions = getNodePositions()
  expect(compactPositions).not.toEqual(initialPositions)
  expect(getLayoutView().density).toEqual("compact")

  act(() => toggleLayoutDensity())
  const comfortablePositions = getNodePositions()
  expect(comfortablePositions).not.toEqual(compactPositions)
  expect(getLayoutView().density).toEqual("comfortable")
})
