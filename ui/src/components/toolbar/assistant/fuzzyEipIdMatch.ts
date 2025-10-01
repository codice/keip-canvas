import { EipComponent } from "../../../api/generated/eipComponentDef"
import { EipId } from "../../../api/generated/eipFlow"
import {
  EIP_SCHEMA,
  lookupEipComponent,
} from "../../../singletons/eipDefinitions"

/**
 * The purpose of this function is to relax matching for EipIds returned by the LLM.
 *
 * Implements a rudimentary fuzzy search that returns immediately if one of the
 * following conditions is true (the provided id is returned if no matches are found):
 * - Check if the given EipId has an exact match in the schema definition
 * - Split the component name into tokens and check for a partial match in the provided namespace
 * - Check if the provided component name has an exact match in the 'integration' namespace
 * - Split the component name into tokens and check for a partial match in the 'integration' namespace
 */
export const fuzzyEipMatch = (id: EipId): EipId => {
  const targetId: EipId = {
    namespace: id.namespace.toLowerCase(),
    name: id.name.toLowerCase(),
  }
  if (lookupEipComponent(targetId)) {
    return targetId
  }

  if (targetId.namespace in EIP_SCHEMA) {
    const match = searchByToken(
      targetId.name.split("-"),
      EIP_SCHEMA[targetId.namespace]
    )
    if (match) {
      return { namespace: targetId.namespace, name: match.eipId.name }
    }
  }

  const intNamespaceId = {
    namespace: "integration",
    name: targetId.name,
  }
  if (lookupEipComponent(intNamespaceId)) {
    return intNamespaceId
  } else {
    const match = searchByToken(
      targetId.name.split("-"),
      EIP_SCHEMA[intNamespaceId.namespace]
    )
    if (match) {
      return { namespace: intNamespaceId.namespace, name: match.eipId.name }
    }
  }

  console.warn(
    `Failed to find a matching component for id: ${JSON.stringify(id)}`
  )
  return id
}

const searchByToken = (
  tokens: string[],
  toSearch: EipComponent[]
): EipComponent | null => {
  for (const token of tokens) {
    const match = toSearch.find((c) => c.eipId.name.includes(token))
    if (match) {
      return match
    }
  }
  return null
}
