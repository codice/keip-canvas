import { DEFAULT_NAMESPACE, RouterKeyDef } from "../api/flow"
import {
  EipComponent,
  EipComponentDefinition,
} from "../api/generated/eipComponentDef"
import { EipId } from "../api/generated/eipFlow"
import eipDefinition from "../json/springIntegrationEipComponents.json"

// TODO: Validate that the parsed JSON matches the schema type
export const EIP_SCHEMA: Readonly<EipComponentDefinition> =
  eipDefinition as EipComponentDefinition

export const eipIdToString = (eipId: EipId) =>
  `${eipId.namespace}.${eipId.name}`

const getFlatMap = (schema: EipComponentDefinition) => {
  const map = new Map<string, EipComponent>()
  for (const [_, componentList] of Object.entries(schema)) {
    componentList.forEach((c) => map.set(eipIdToString(c.eipId), c))
  }
  return map
}

const componentFlatMap = getFlatMap(EIP_SCHEMA)

export const lookupEipComponent = (eipId: EipId) => {
  const component = componentFlatMap.get(eipIdToString(eipId))
  if (component === undefined) {
    console.warn(`Did not find component with id: ${JSON.stringify(eipId)}`)
  }
  return component
}

// TODO: Make the following attribute specific settings configurable via the EipComponentDefinition API
export const FLOW_CONTROLLED_ATTRIBUTES = new Set([
  "id",
  "channel",
  "input-channel",
  "output-channel",
  "discard-channel",
  "request-channel",
  "reply-channel",
  "default-output-channel",
])

export const DYNAMIC_ROUTING_CHILDREN = new Set(["mapping", "recipient"])

export const CHANNEL_ATTR_NAME = "channel"
export const DEFAULT_OUTPUT_CHANNEL_NAME = "default-output-channel"

interface RouterTarget {
  name: string
  type: "attribute" | "child"
}

// TODO: Make the following attribute specific settings configurable via the EipComponentDefinition API
const contentBasedRouterTargets: ReadonlyMap<string, RouterTarget> = new Map([
  [
    "integration.header-value-router",
    { name: "header-name", type: "attribute" },
  ],
  ["integration.router", { name: "expression", type: "attribute" }],
  ["int-xml.xpath-router", { name: "xpath-expression", type: "child" }],
])

export const lookupContentBasedRouterKeys = (
  eipId: EipId
): RouterKeyDef | null => {
  const target = contentBasedRouterTargets.get(eipIdToString(eipId))
  if (!target) {
    return null
  }

  const eipComponent = lookupEipComponent(eipId)

  // TODO: Reduce the sprawl of this switch statement.
  switch (target.type) {
    case "attribute": {
      const attrKey = eipComponent?.attributes?.find(
        (attr) => attr.name === target.name
      )
      if (!attrKey) {
        throw new Error(
          `The router "${eipId.name}" does not have an attribute with the name "${target.name}"`
        )
      }
      return {
        eipId: { namespace: DEFAULT_NAMESPACE, name: attrKey.name },
        type: target.type,
        attributesDef: [attrKey],
      }
    }
    case "child": {
      const childKey = eipComponent?.childGroup?.children.find(
        (child) => child.eipId.name === target.name
      )
      if (!childKey) {
        throw new Error(
          `The router "${eipId.name}" does not have a child with the name "${target.name}"`
        )
      }
      return {
        eipId: childKey.eipId,
        type: target.type,
        attributesDef: childKey.attributes ?? [],
      }
    }
  }
}
