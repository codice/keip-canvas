import {
  Button,
  ContainedList,
  ContainedListItem,
  FormLabel,
  Modal,
  OverflowMenu,
  OverflowMenuItem,
  Stack,
  TextInput,
} from "@carbon/react"

import {
  AddLarge,
  Close,
  Maximize,
  Minimize,
  Settings,
} from "@carbon/react/icons"
import { InlineLoadingStatus } from "@carbon/react/lib/components/InlineLoading/InlineLoading"
import { useState } from "react"
import { createPortal } from "react-dom"
import {
  clearAllCustomEntities,
  removeCustomEntity,
  updateCustomEntity,
} from "../../singletons/store/appActions"
import { useGetCustomEntityIds } from "../../singletons/store/getterHooks"
import { getCustomEntityContent } from "../../singletons/store/storeViews"
import { ModalCodeEditor } from "../editor/ModalCodeEditor"

interface CustomEntityPanelProps {
  isCollapsed: boolean
  setCollapsed: (isCollapsed: boolean) => void
}

interface ExpandedPanelProps {
  onAddEntity: () => void
  onCollapsePanel: () => void
}

interface CollapsedPanelProps {
  onExpandPanel: () => void
}

interface CreateEntityModalProps {
  entityId: string | null
  open: boolean
  setOpen: (open: boolean) => void
}

const CONTENT_HELPER_TEXT =
  "Note: The root element’s ID is set automatically from the Entity ID above. No need to add an 'id' attribute manually."

const ExpandedPanelActions = ({
  onAddEntity,
  onCollapsePanel,
}: ExpandedPanelProps) => (
  <>
    <Button
      kind="ghost"
      size="lg"
      iconDescription="Collapse"
      hasIconOnly
      renderIcon={Minimize}
      tooltipPosition="left"
      onClick={onCollapsePanel}
    />
    <OverflowMenu
      align="left"
      iconDescription="Options"
      renderIcon={Settings}
      size="lg"
    >
      <OverflowMenuItem
        itemText="Clear all"
        isDelete
        onClick={() => clearAllCustomEntities()}
      />
    </OverflowMenu>
    <Button
      kind="primary"
      size="lg"
      iconDescription="Add New Entity"
      hasIconOnly
      renderIcon={AddLarge}
      tooltipPosition="left"
      onClick={onAddEntity}
    />
  </>
)

const CollapsedPanelActions = ({ onExpandPanel }: CollapsedPanelProps) => (
  <Button
    kind="ghost"
    size="lg"
    iconDescription="Expand"
    hasIconOnly
    renderIcon={Maximize}
    tooltipPosition="left"
    onClick={onExpandPanel}
  />
)

export const CustomEntityPanel = ({
  isCollapsed,
  setCollapsed,
}: CustomEntityPanelProps) => {
  const [entityModalOpen, setEntityModalOpen] = useState(false)
  const [selectedEntity, setSelectedEntity] = useState<string | null>(null)
  const entityIds = useGetCustomEntityIds()

  const entityIdsToListItems = (ids: string[]) =>
    ids.sort().map((id) => (
      <ContainedListItem
        key={id}
        action={
          <Button
            kind="ghost"
            iconDescription="Delete"
            hasIconOnly
            renderIcon={Close}
            tooltipPosition="left"
            onClick={() => removeCustomEntity(id)}
          />
        }
        onClick={() => {
          setSelectedEntity(id)
          setEntityModalOpen(true)
        }}
      >
        {id}
      </ContainedListItem>
    ))

  return (
    <>
      <ContainedList
        label="Entities"
        size="lg"
        kind="on-page"
        action={
          isCollapsed ? (
            <CollapsedPanelActions onExpandPanel={() => setCollapsed(false)} />
          ) : (
            <ExpandedPanelActions
              onAddEntity={() => {
                setSelectedEntity(null)
                setEntityModalOpen(true)
              }}
              onCollapsePanel={() => setCollapsed(true)}
            />
          )
        }
      >
        {!isCollapsed && entityIdsToListItems(entityIds)}
      </ContainedList>

      {entityModalOpen && (
        <CreateEntityModal
          key={selectedEntity}
          entityId={selectedEntity}
          open={entityModalOpen}
          setOpen={setEntityModalOpen}
        />
      )}
    </>
  )
}

const getLoadingDescription = (status: InlineLoadingStatus) => {
  switch (status) {
    case "active":
      return "Saving"
    case "finished":
      return "Saved"
    case "error":
      return "Failed to save"
    case "inactive":
      return ""
  }
}

const getEntityContentFromId = (entityId: string | null) => {
  if (!entityId) {
    return ""
  }
  return getCustomEntityContent(entityId) ?? ""
}

const CreateEntityModal = ({
  entityId,
  open,
  setOpen,
}: CreateEntityModalProps) => {
  const [localId, setLocalId] = useState(entityId ?? "")
  const [content, setContent] = useState(() => getEntityContentFromId(entityId))
  const [loadingStatus, setLoadingStatus] =
    useState<InlineLoadingStatus>("inactive")
  const [idErrorMessage, setIdErrorMessage] = useState("")
  const [contentErrorMessage, setContentErrorMessage] = useState("")

  const resetAndCloseModal = () => {
    setOpen(false)
    setLoadingStatus("inactive")
  }

  const saveUpdates = () => {
    setLoadingStatus("active")

    const result = updateCustomEntity(entityId, localId, content)

    if (!result.success) {
      setLoadingStatus("error")
      setIdErrorMessage(result.idError ?? "")
      setContentErrorMessage(result.contentError ?? "")
    } else {
      resetAndCloseModal()
    }
  }

  const resetLoadingStatus = () =>
    loadingStatus !== "inactive" && setLoadingStatus("inactive")

  const updateId = (id: string) => {
    resetLoadingStatus()
    setLocalId(id)
  }

  const updateContent = (content: string) => {
    resetLoadingStatus()
    setContent(content)
  }

  const isErrorState = loadingStatus === "error"

  return createPortal(
    <Modal
      open={open}
      onRequestClose={resetAndCloseModal}
      size="md"
      modalHeading={
        entityId ? "Update a custom entity" : "Create a custom entity"
      }
      modalLabel={entityId}
      primaryButtonText="Save"
      secondaryButtonText="Cancel"
      loadingStatus={loadingStatus}
      loadingDescription={getLoadingDescription(loadingStatus)}
      onRequestSubmit={saveUpdates}
    >
      <Stack gap={5}>
        <p>
          Custom entities do not appear on the flow diagram, but can be
          referenced by flow component attributes.
        </p>

        <TextInput
          data-modal-primary-focus
          id="entity-id-input"
          labelText="Entity ID"
          value={localId}
          helperText="Must be unique"
          onChange={(e) => updateId(e.target.value)}
          invalid={isErrorState && idErrorMessage !== ""}
          invalidText={idErrorMessage}
          onKeyDown={(e) => e.key === "Enter" && saveUpdates()}
        />

        <Stack gap={3}>
          <FormLabel>Content</FormLabel>
          <ModalCodeEditor
            content={content}
            setContent={updateContent}
            language="xml"
            helperText={CONTENT_HELPER_TEXT}
            invalid={isErrorState && contentErrorMessage !== ""}
            invalidText={contentErrorMessage}
          />
        </Stack>
      </Stack>
    </Modal>,
    document.body
  )
}
