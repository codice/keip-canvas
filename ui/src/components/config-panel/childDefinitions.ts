import isDeepEqual from "fast-deep-equal"
import {
  EipChildElement,
  EipComponent,
} from "../../api/generated/eipComponentDef"
import { getEipId } from "../../singletons/store/storeViews"

// Searches the root's child tree to find the child definition referenced by 'childPath'
export const findChildDefinition = (
  rootEipComponent: EipComponent,
  childPath: string[]
) => {
  let children = rootEipComponent.childGroup?.children
  let child: EipChildElement | null = null

  if (childPath.length <= 1) {
    return null
  }

  for (const id of childPath.slice(1)) {
    const eipId = getEipId(id)
    child = children?.find((c) => isDeepEqual(c.eipId, eipId)) ?? null
    children = child?.childGroup?.children
  }

  return child
}
