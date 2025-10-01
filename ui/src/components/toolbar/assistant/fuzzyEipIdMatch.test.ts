import { expect, test } from "vitest"
import { DEFAULT_NAMESPACE } from "../../../api/flow"
import { fuzzyEipMatch } from "./fuzzyEipIdMatch"

test("exact match", () => {
  const eipId = { namespace: DEFAULT_NAMESPACE, name: "filter" }
  const result = fuzzyEipMatch(eipId)
  expect(result).toEqual(eipId)
})

test("partial match in the same namespace", () => {
  const eipId = { namespace: "jms", name: "test-adapter" }
  const result = fuzzyEipMatch(eipId)
  expect(result).toEqual({ namespace: "jms", name: "inbound-channel-adapter" })
})

test("exact name match in default namespace", () => {
  const eipId = { namespace: "jms", name: "aggregator" }
  const result = fuzzyEipMatch(eipId)
  expect(result).toEqual({ namespace: DEFAULT_NAMESPACE, name: "aggregator" })
})

test("no partial match in the same namespace -> fallback to default namespace", () => {
  const eipId = { namespace: "jms", name: "test-split-example" }
  const result = fuzzyEipMatch(eipId)
  expect(result).toEqual({ namespace: DEFAULT_NAMESPACE, name: "splitter" })
})

test("no partial matches -> return same input EipId", () => {
  const eipId = { namespace: "jms", name: "unknown" }
  const result = fuzzyEipMatch(eipId)
  expect(result).toEqual({ namespace: "jms", name: "unknown" })
})

test("unknown namespace -> return same input EipId", () => {
  const eipId = { namespace: "ns_unknown", name: "unknown" }
  const result = fuzzyEipMatch(eipId)
  expect(result).toEqual({ namespace: "ns_unknown", name: "unknown" })
})
