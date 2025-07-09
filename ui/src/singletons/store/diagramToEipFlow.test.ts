import { act, renderHook } from "@testing-library/react"
import { expect, test, vi } from "vitest"
import { useEipFlow } from "./diagramToEipFlow"
import { resetMockStore } from "./storeTestingUtils"
import childBasedRouterFlow from "./testdata/store-initializers/childBasedRouterFlow.json"
import inboundGatewayFlow from "./testdata/store-initializers/inboundGatewayFlow.json"
import nestedChildFlow from "./testdata/store-initializers/nestedChildFlow.json"
import standardFlow from "./testdata/store-initializers/standardFlow.json"

vi.mock("zustand")

test("standard diagram to EipFlow success", () => {
  act(() => {
    resetMockStore(standardFlow)
  })

  const { result } = renderHook(() => useEipFlow())
  expect(result.current).toMatchSnapshot()
})

test("diagram includes a router with child based matcher to EipFlow success", () => {
  act(() => {
    resetMockStore(childBasedRouterFlow)
  })

  const { result } = renderHook(() => useEipFlow())
  expect(result.current).toMatchSnapshot()
})

test("diagram with deep child nesting", () => {
  act(() => {
    resetMockStore(nestedChildFlow)
  })

  const { result } = renderHook(() => useEipFlow())
  expect(result.current).toMatchSnapshot()
})

test("diagram with inbound-request-reply node", () => {
  act(() => {
    resetMockStore(inboundGatewayFlow)
  })

  const { result } = renderHook(() => useEipFlow())
  expect(result.current).toMatchSnapshot()
})
