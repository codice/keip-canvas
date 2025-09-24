import { beforeEach, describe, expect, test, vi } from "vitest"
import { EipFlow } from "../../api/generated/eipFlow"
import { eipFlowToDiagram } from "./eipFlowToDiagram"
import customEntityFlow from "./testdata/eipFlows/customEntity.json"
import inboundGatewayFlow from "./testdata/eipFlows/inboundGateway.json"
import nestedChildrenFlow from "./testdata/eipFlows/nestedChildren.json"
import payloadTypeRouterFlow from "./testdata/eipFlows/routers/payloadTypeRouter.json"
import recipientListRouterFlow from "./testdata/eipFlows/routers/recipientListRouter.json"
import simpleRouterFlow from "./testdata/eipFlows/routers/simpleRouter.json"
import xpathRouterFlow from "./testdata/eipFlows/routers/xpathRouter.json"
import simpleFlow from "./testdata/eipFlows/simple.json"

vi.mock("zustand")

let nodeCount = 0
let childCount = 0

// Generate predictable node ids
vi.mock("../../utils/nodeIdGenerator", () => ({
  generateChildId: () => `c${childCount++}`,
  generateNodeId: () => `n${nodeCount++}`,
}))

beforeEach(() => {
  nodeCount = 0
  childCount = 0
})

describe("EipFlow to diagram", () => {
  test.each([
    {
      msg: "simple flow",
      flow: simpleFlow,
    },
    {
      msg: "nested children",
      flow: nestedChildrenFlow,
    },
    {
      msg: "simple router",
      flow: simpleRouterFlow,
    },
    {
      msg: "payload-type router",
      flow: payloadTypeRouterFlow,
    },
    {
      msg: "recipient-list router",
      flow: recipientListRouterFlow,
    },
    {
      msg: "xpath router",
      flow: xpathRouterFlow,
    },
    {
      msg: "inbound gateway",
      flow: inboundGatewayFlow,
    },
    {
      msg: "custom entity",
      flow: customEntityFlow,
    },
  ])("$msg", ({ flow }) => {
    const flowDiagram = eipFlowToDiagram(flow as unknown as EipFlow)
    expect(flowDiagram).toMatchSnapshot()
  })
})
