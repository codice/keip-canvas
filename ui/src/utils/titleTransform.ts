import { DEFAULT_NAMESPACE } from "../api/flow"
import { EipId } from "../api/generated/eipFlow"

const EIP_NAMESPACE_ALIASES: Record<string, string> = {
  "int-xml": "xml",
  ws: "web-services",
}

export const toTitleCase = (str: string) =>
  str
    .toLowerCase()
    .split("-")
    .map((s) => s.charAt(0).toUpperCase() + s.substring(1))
    .join(" ")

export const getNamespaceAlias = (ns: string) => EIP_NAMESPACE_ALIASES[ns] ?? ns

export const getNamespacedTitle = (eipId: EipId) => {
  if (eipId.namespace === DEFAULT_NAMESPACE) {
    return toTitleCase(eipId.name)
  }

  const namespace = getNamespaceAlias(eipId.namespace)
  return toTitleCase(namespace) + " " + toTitleCase(eipId.name)
}
