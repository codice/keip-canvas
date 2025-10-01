import { HeaderPanel } from "@carbon/react"
import { useOnSelectionChange } from "@xyflow/react"
import { useState } from "react"
import {
  CustomEdge,
  CustomNode,
  DynamicEdge,
  isDynamicEdge,
  isEipNode,
  isFollowerNode,
} from "../../api/flow"
import { Attribute } from "../../api/generated/eipComponentDef"
import { EipId } from "../../api/generated/eipFlow"
import {
  FLOW_CONTROLLED_ATTRIBUTES,
  lookupContentBasedRouterKeys,
  lookupEipComponent,
} from "../../singletons/eipDefinitions"
import { clearSelectedChildNode } from "../../singletons/store/appActions"
import { useGetSelectedChildNode } from "../../singletons/store/getterHooks"
import { getEipId } from "../../singletons/store/storeViews"
import DynamicEdgeConfig from "./DynamicEdgeConfig"
import {
  ChildNodeConfig,
  FollowerNodePanel,
  RootNodeConfig,
} from "./NodeConfigPanel"
import { findChildDefinition } from "./childDefinitions"

const isDynamicRouterAttribute = (attribute: Attribute, eipId?: EipId) => {
  if (!eipId) {
    return false
  }

  const keyDef = lookupContentBasedRouterKeys(eipId)
  if (!keyDef) {
    return false
  }

  return keyDef.type === "attribute" && keyDef.eipId.name === attribute.name
}

const filterConfigurableAttributes = (attrs?: Attribute[], eipId?: EipId) => {
  return attrs
    ? attrs.filter(
        (attr) =>
          !FLOW_CONTROLLED_ATTRIBUTES.has(attr.name) &&
          !isDynamicRouterAttribute(attr, eipId)
      )
    : []
}

const EipConfigSidePanel = () => {
  const [selectedNode, setSelectedNode] = useState<CustomNode | null>(null)
  const [selectedEdge, setSelectedEdge] = useState<DynamicEdge | null>(null)
  const selectedChildPath = useGetSelectedChildNode()

  useOnSelectionChange<CustomNode, CustomEdge>({
    onChange: ({ nodes, edges }) => {
      selectedChildPath && clearSelectedChildNode()
      const numSelected = nodes.length + edges.length
      setSelectedNode(numSelected === 1 ? nodes[0] : null)
      setSelectedEdge(
        numSelected === 1 && isDynamicEdge(edges[0]) ? edges[0] : null
      )
    },
  })

  let sidePanelContent
  if (selectedChildPath) {
    // TODO: Handle error case if childElement or rootComponent is undefined
    const childId = selectedChildPath[selectedChildPath.length - 1]
    const rootComponent = lookupEipComponent(getEipId(selectedChildPath[0])!)!
    const childElement = findChildDefinition(rootComponent, selectedChildPath)
    const configurableAttrs = filterConfigurableAttributes(
      childElement?.attributes
    )
    sidePanelContent = (
      <ChildNodeConfig
        key={childId}
        childPath={selectedChildPath}
        attributes={configurableAttrs}
      />
    )
  } else if (selectedNode && isEipNode(selectedNode)) {
    // TODO: Handle error case if eipComponent is undefined
    const selectedNodeEipId = selectedNode && getEipId(selectedNode.id)
    const eipComponent =
      selectedNodeEipId && lookupEipComponent(selectedNodeEipId)
    const configurableAttrs = filterConfigurableAttributes(
      eipComponent?.attributes,
      selectedNodeEipId
    )
    sidePanelContent = (
      <RootNodeConfig
        key={selectedNode.id}
        node={selectedNode}
        attributes={configurableAttrs}
      />
    )
  } else if (selectedNode && isFollowerNode(selectedNode)) {
    sidePanelContent = <FollowerNodePanel node={selectedNode} />
  } else if (selectedEdge) {
    sidePanelContent = (
      <DynamicEdgeConfig key={selectedEdge.id} edge={selectedEdge} />
    )
  } else {
    // Returning an empty fragment because the HeaderPanel component spams
    // the logs with error messages if it doesn't have any children.
    sidePanelContent = <></>
  }

  const showPanel = Boolean(selectedNode || selectedChildPath || selectedEdge)

  return (
    <HeaderPanel
      className={showPanel ? "node-config-panel" : ""}
      expanded={showPanel}
    >
      {sidePanelContent}
    </HeaderPanel>
  )
}

export default EipConfigSidePanel
