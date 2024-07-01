import { Button, Stack, Tile } from "@carbon/react"
import { Handle, NodeProps, Position } from "reactflow"
import { ServiceId } from "@carbon/react/icons"
import { FlowType, Role } from "../../api/eipSchema"
import { EipNodeData, Layout} from "../../api/flow"
import { ChildNodeId, EipId } from "../../api/id"
import { lookupEipComponent } from "../../singletons/eipDefinitions"
import getIconUrl from "../../singletons/eipIconCatalog"
import {
  useAppActions,
  useGetChildren,
  useIsChildSelected,
  useGetLayout
} from "../../singletons/store"
import { toTitleCase } from "../../utils/titleTransform"
import "./nodes.scss"

interface ChildrenIconsProps {
  childrenNames: string[]
  parentNodeId: string
  parentEipId: EipId
}

const defaultNamespace = "integration"

// TODO: Limit handles to the appropriate number of connections
const renderHandles = (flowType: FlowType, layoutType: Layout["orientation"]) => {
  if (layoutType === "horizontal") {
    switch (flowType) {
      case "source":
        return <Handle type="source" position={Position.Right}></Handle>
      case "sink":
        return <Handle type="target" position={Position.Left}></Handle>
      case "passthru":
        return (
          <>
            <Handle type="source" position={Position.Right}></Handle>
            <Handle type="target" position={Position.Left}></Handle>
          </>
        )
      default:
        console.error("unhandled FlowType")
    }
  } else {
    switch (flowType) {
      case "source":
        return <Handle type="source" position={Position.Bottom}></Handle>
      case "sink":
        return <Handle type="target" position={Position.Top}></Handle>
      case "passthru":
        return (
          <>
            <Handle type="source" position={Position.Bottom}></Handle>
            <Handle type="target" position={Position.Top}></Handle>
          </>
        )
      default:
        console.error("unhandled FlowType")
    }
  }
}

const getNamespacedTitle = (eipId: EipId) => {
  if (eipId.namespace === defaultNamespace) {
    return toTitleCase(eipId.name)
  }
  return toTitleCase(eipId.namespace) + " " + toTitleCase(eipId.name)
}

const getClassNames = (props: NodeProps<EipNodeData>, role: Role) => {
  const roleClsName =
    role === "channel" ? "eip-channel-node" : "eip-endpoint-node"
  const selectedClsName = props.selected ? "eip-node-selected" : ""
  return ["eip-node", roleClsName, selectedClsName].join(" ")
}

const ChildIconButton = (props: ChildNodeId) => {
  const { updateSelectedChildNode } = useAppActions()
  const selected = useIsChildSelected(props)

  const clsNames = ["child-icon-button"]
  selected && clsNames.push("child-icon-button-focused")

  return (
    <Button
      className={clsNames.join(" ")}
      hasIconOnly
      renderIcon={ServiceId}
      iconDescription={props.name}
      size="sm"
      tooltipPosition="bottom"
      kind="primary"
      onClick={(ev) => {
        ev.stopPropagation()
        updateSelectedChildNode(props)
      }}
    />
  )
}

// TODO: Account for a large number of children to be displayed
// TODO: Create a mapping of children to icons (with a fallback option)
const ChildrenIcons = ({ childrenNames, parentNodeId }: ChildrenIconsProps) => {
  return (
    <Stack className="eip-node-children" orientation="horizontal" gap={2}>
      {childrenNames.map((name) => (
        <ChildIconButton key={name} name={name} parentNodeId={parentNodeId} />
      ))}
    </Stack>
  )
}

// TODO: Consider separating into Endpoint and Channel custom node types
export const EipNode = (props: NodeProps<EipNodeData>) => {
  // TODO: clearSelectedChildNode is used in too many different components. See if that can be reduced (or elimnated).
  const { clearSelectedChildNode } = useAppActions()
  const childrenState = useGetChildren(props.id)
  const hasChildren = childrenState.length > 0

  const { data } = props
  const componentDefinition = lookupEipComponent(data.eipId)!
  const layout : Layout = useGetLayout()
  const handles = renderHandles(componentDefinition.flowType, layout.orientation)

  return (
    <Tile
      className={getClassNames(props, componentDefinition.role)}
      onClick={hasChildren ? () => clearSelectedChildNode() : undefined}
    >
      <div>{getNamespacedTitle(data.eipId)}</div>
      <img className="eip-node-image" src={getIconUrl(data.eipId)} />
      <div style={hasChildren ? { paddingBottom: "0.5rem" } : {}}>
        <strong>{data.label}</strong>
      </div>
      {hasChildren && (
        <ChildrenIcons
          childrenNames={childrenState}
          parentNodeId={props.id}
          parentEipId={props.data.eipId}
        />
      )}
      {handles}
    </Tile>
  )
}
