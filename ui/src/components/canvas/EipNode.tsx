import { Tile } from "@carbon/react"

import { Handle, NodeProps, Position, useNodesData } from "@xyflow/react"
import {
  EipFlowNode,
  FollowerNode as FollowerNodeType,
  Layout,
} from "../../api/flow"
import { ConnectionType, EipRole } from "../../api/generated/eipComponentDef"
import { EipId } from "../../api/generated/eipFlow"
import {
  eipIdToString,
  lookupEipComponent,
} from "../../singletons/eipDefinitions"
import getIconUrl from "../../singletons/eipIconCatalog"
import { describeFollower } from "../../singletons/followerNodeDefs"
import { clearSelectedChildNode } from "../../singletons/store/appActions"
import {
  useGetEnabledChildren,
  useGetLayout,
} from "../../singletons/store/getterHooks"
import { getEipId } from "../../singletons/store/storeViews"
import { getNamespacedTitle } from "../../utils/titleTransform"
import { ChildrenNavigationPopover } from "./ChildrenNavigationPopover"
import "./nodes.scss"

interface NodeContentProps {
  eipId: EipId
  label: string
}

const DEFAULT_NODE_LABEL = "New Node"

const renderHorizontalHandles = (connectionType: ConnectionType) => {
  switch (connectionType) {
    case "source":
    case "inbound_request_reply":
      return <Handle id="output" type="source" position={Position.Right} />
    case "sink":
      return <Handle id="input" type="target" position={Position.Left} />
    case "content_based_router":
    case "passthru":
    case "request_reply":
      return (
        <>
          <Handle id="output" type="source" position={Position.Right} />
          <Handle id="input" type="target" position={Position.Left} />
        </>
      )
    case "tee":
      return (
        <>
          <Handle id="output" type="source" position={Position.Right} />
          <Handle id="discard" type="source" position={Position.Bottom} />
          <Handle id="input" type="target" position={Position.Left} />
        </>
      )
  }
}

const renderVerticalHandles = (connectionType: ConnectionType) => {
  switch (connectionType) {
    case "source":
    case "inbound_request_reply":
      return <Handle id="output" type="source" position={Position.Bottom} />
    case "sink":
      return <Handle id="input" type="target" position={Position.Top} />
    case "content_based_router":
    case "passthru":
    case "request_reply":
      return (
        <>
          <Handle id="output" type="source" position={Position.Bottom} />
          <Handle id="input" type="target" position={Position.Top} />
        </>
      )
    case "tee":
      return (
        <>
          <Handle id="output" type="source" position={Position.Bottom} />
          <Handle id="discard" type="source" position={Position.Right} />
          <Handle id="input" type="target" position={Position.Top} />
        </>
      )
  }
}

// TODO: Limit handles to the appropriate number of connections
const renderHandles = (
  connectionType: ConnectionType,
  layoutType: Layout["orientation"]
) =>
  layoutType === "horizontal"
    ? renderHorizontalHandles(connectionType)
    : renderVerticalHandles(connectionType)

const getNodeClassNames = (isSelected: boolean, role: EipRole) => {
  const roleClsName =
    role === "channel" ? "eip-channel-node" : "eip-endpoint-node"
  const selectedClsName = isSelected ? "eip-node-selected" : ""
  return ["eip-node", roleClsName, selectedClsName].join(" ")
}

const NodeDisplayContent = ({ eipId, label }: NodeContentProps) => (
  <>
    <div className="eip-node-title">{getNamespacedTitle(eipId)}</div>
    <img className="eip-node-image" src={getIconUrl(eipId)} />
    <div className="eip-node-label">
      <strong>{label}</strong>
    </div>
  </>
)

export const EipNode = (props: NodeProps<EipFlowNode>) => {
  const layout = useGetLayout()
  const children = useGetEnabledChildren(props.id)
  const hasChildren = children.length > 0

  const eipId = getEipId(props.id)
  const componentDefinition = eipId && lookupEipComponent(eipId)
  if (!componentDefinition) {
    return null
  }

  const handles = renderHandles(
    componentDefinition.connectionType,
    layout.orientation
  )

  const { data } = props

  return (
    <Tile
      className={getNodeClassNames(props.selected, componentDefinition.role)}
      onClick={hasChildren ? () => clearSelectedChildNode() : undefined}
    >
      <NodeDisplayContent
        eipId={eipId}
        label={data.label || DEFAULT_NODE_LABEL}
      />
      {hasChildren && <ChildrenNavigationPopover />}
      {handles}
    </Tile>
  )
}

export const FollowerNode = (props: NodeProps<FollowerNodeType>) => {
  const layout = useGetLayout()
  const leaderId = props.data.leaderId
  const leaderData = useNodesData<EipFlowNode>(leaderId)

  const eipId = getEipId(props.id)
  const componentDefinition = eipId && lookupEipComponent(eipId)

  const leaderEipId = getEipId(leaderId)

  if (!componentDefinition || !leaderEipId) {
    console.error(
      `Failed to render follower node with eipId: (${eipId && eipIdToString(eipId)}) for leaderId: (${leaderId})`
    )
    return null
  }

  const followerDescriptor = describeFollower(leaderEipId)

  if (!followerDescriptor) {
    console.error(
      `Failed to find follower descriptor for node with eipId: (${eipIdToString(eipId)}) for leaderId: (${leaderId})`
    )
    return null
  }

  const handles = renderHandles(
    followerDescriptor.overrides?.connectionType ??
      componentDefinition.connectionType,
    layout.orientation
  )

  return (
    <Tile
      className={
        getNodeClassNames(props.selected, componentDefinition.role) +
        " eip-follower-node"
      }
    >
      <NodeDisplayContent
        eipId={eipId}
        label={followerDescriptor.generateLabel(
          leaderData?.data.label ?? leaderId
        )}
      />
      {handles}
    </Tile>
  )
}
