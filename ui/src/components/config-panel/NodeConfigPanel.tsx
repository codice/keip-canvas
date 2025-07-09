import { Button, Stack, TextArea, TextInput } from "@carbon/react"
import { ParentNode, Settings } from "@carbon/react/icons"
import { ChangeEvent, useMemo, useState } from "react"
import { EipFlowNode, FollowerNode } from "../../api/flow"
import { Attribute } from "../../api/generated/eipComponentDef"
import {
  switchNodeSelection,
  updateNodeDescription,
  updateNodeLabel,
} from "../../singletons/store/appActions"
import { useGetNodeDescription } from "../../singletons/store/getterHooks"
import { getEipId } from "../../singletons/store/storeViews"
import debounce from "../../utils/debounce"
import { getNamespacedTitle } from "../../utils/titleTransform"
import { AttributeConfigForm } from "./AttributeConfigForm"
import { ChildManagementModal } from "./ChildManagementModal"
import ConfigurationInputTabs from "./ConfigurationTabs"

interface IdDisplayProps {
  id: string
}

interface LabelInputProps {
  node: EipFlowNode
}

interface DescriptionInputProps {
  id: string
}

interface BasePanelProps {
  id: string
  attributes: Attribute[]
  metadataInputs: React.ReactNode
  childPath?: string[]
}

interface RootPanelProps {
  node: EipFlowNode
  attributes: Attribute[]
}

interface ChildPanelProps {
  childPath: string[]
  attributes: Attribute[]
}

interface FollowerPanelProps {
  node: FollowerNode
}

const IdDisplay = ({ id }: IdDisplayProps) => (
  <TextInput id="nodeId" labelText="NodeId" disabled defaultValue={id} />
)

const LabelInput = ({ node }: LabelInputProps) => {
  const [isLabelValid, setIsLabelValid] = useState(true)

  const handleLabelUpdates = useMemo(
    () =>
      debounce((ev: ChangeEvent<HTMLInputElement>) => {
        const err = updateNodeLabel(node.id, ev.target.value)
        setIsLabelValid(!err)
      }, 300),
    [node.id]
  )

  return (
    <TextInput
      id="nodeLabel"
      labelText="Label"
      defaultValue={node ? node.data.label : ""}
      enableCounter
      invalid={!isLabelValid}
      invalidText="Node labels must be unique"
      maxCount={60}
      placeholder="New Node"
      onChange={handleLabelUpdates}
    />
  )
}

const DescriptionInput = ({ id }: DescriptionInputProps) => {
  const description = useGetNodeDescription(id)

  const handleDescriptionUpdates = useMemo(
    () =>
      debounce(
        (ev: ChangeEvent<HTMLTextAreaElement>) =>
          updateNodeDescription(id, ev.target.value),
        300
      ),
    [id]
  )

  return (
    <TextArea
      labelText="Description"
      helperText="Optional description of the selected node's behavior"
      enableCounter
      maxCount={600}
      defaultValue={description}
      onChange={handleDescriptionUpdates}
    ></TextArea>
  )
}

const BaseNodePanel = ({
  id,
  attributes,
  metadataInputs,
  childPath,
}: BasePanelProps) => {
  const [childModalOpen, setChildModalOpen] = useState(false)

  return (
    <Stack gap={8} className="cfg-panel__container__top-padding-add">
      {metadataInputs}

      <div className="cfg-panel__container__side-padding-add">
        <Button
          className="cfg-panel__button"
          kind="tertiary"
          onClick={() => childModalOpen || setChildModalOpen(true)}
          renderIcon={Settings}
        >
          Configure Children
        </Button>
      </div>

      <ConfigurationInputTabs
        hasAttributes={attributes.length > 0}
        attributesForm={<AttributeConfigForm id={id} attrs={attributes} />}
      />

      {childModalOpen && (
        <ChildManagementModal
          rootId={childPath ? childPath[0] : id}
          open={childModalOpen}
          setOpen={setChildModalOpen}
          initPath={childPath}
        />
      )}
    </Stack>
  )
}

export const RootNodeConfig = ({ node, attributes }: RootPanelProps) => {
  const eipId = getEipId(node.id)

  const metadataInputs = (
    <Stack gap={6} className="cfg-panel__container__side-padding-add">
      {eipId && <h4>{getNamespacedTitle(eipId)}</h4>}
      <IdDisplay id={node.id} />
      <LabelInput node={node} />
      <DescriptionInput id={node.id} />
    </Stack>
  )

  return (
    <BaseNodePanel
      id={node.id}
      attributes={attributes}
      metadataInputs={metadataInputs}
    />
  )
}

export const ChildNodeConfig = ({ childPath, attributes }: ChildPanelProps) => {
  const childId = childPath[childPath.length - 1]
  const eipId = getEipId(childId)

  const metadataInputs = (
    <Stack gap={6} className="cfg-panel__container__side-padding-add">
      {eipId && <h4>{getNamespacedTitle(eipId)}</h4>}
      <IdDisplay id={childId} />
      <DescriptionInput id={childId} />
    </Stack>
  )

  return (
    <BaseNodePanel
      id={childId}
      attributes={attributes}
      metadataInputs={metadataInputs}
      childPath={childPath}
    />
  )
}

export const FollowerNodePanel = ({ node }: FollowerPanelProps) => {
  const eipId = getEipId(node.id)

  return (
    <div className="cfg-panel__container__top-padding-add">
      <Stack gap={6} className="cfg-panel__container__side-padding-add">
        {eipId && <h4>{getNamespacedTitle(eipId)}</h4>}
        <IdDisplay id={node.id} />
        <h5>This node is managed by its parent</h5>
        <Button
          className="cfg-panel__button"
          kind="primary"
          onClick={() => switchNodeSelection(node.data.leaderId)}
          renderIcon={ParentNode}
        >
          Go to Parent
        </Button>
      </Stack>
    </div>
  )
}
