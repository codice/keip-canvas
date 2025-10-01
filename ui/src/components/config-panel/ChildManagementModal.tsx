import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  Dropdown,
  Layer,
  Modal,
  Stack,
} from "@carbon/react"

import { Close, SettingsEdit } from "@carbon/react/icons"
import { ReactNode, useState } from "react"
import { createPortal } from "react-dom"
import {
  EipChildElement,
  EipComponent,
} from "../../api/generated/eipComponentDef"
import { lookupEipComponent } from "../../singletons/eipDefinitions"
import {
  disableChild,
  enableChild,
  reorderEnabledChildren,
  updateSelectedChildNode,
} from "../../singletons/store/appActions"
import { useGetEnabledChildren } from "../../singletons/store/getterHooks"
import { getEipId } from "../../singletons/store/storeViews"
import { DraggableList, DraggableListItem } from "./DraggableList"
import { findChildDefinition } from "./childDefinitions"

interface ChildPathBreadcrumbProps {
  path: string[]
  navigatePath: (idx: number) => void
}

interface ChildSelectorProps {
  parentId: string
  childOptions: EipChildElement[]
}

interface ChildrenDisplayProps {
  parentId: string
  enabledChildren: string[]
  updatePath: (childId: string) => void
  openChildConfigPanel: (childId: string) => void
}

interface ChildModalProps {
  rootId: string
  open: boolean
  setOpen: (open: boolean) => void
  initPath?: string[]
}

const getChildEipDefinition = (rootEipDef: EipComponent, path: string[]) => {
  if (path.length == 1) {
    return rootEipDef.childGroup?.children
  }

  const child = findChildDefinition(rootEipDef, path)
  return child?.childGroup?.children
}

const ChildPathBreadcrumb = ({
  path,
  navigatePath,
}: ChildPathBreadcrumbProps) => (
  <Breadcrumb>
    {path.map((id, idx) => (
      <BreadcrumbItem
        className="breadcrumb__item"
        key={idx}
        onClick={() => navigatePath(idx)}
      >
        {getEipId(id)?.name}
      </BreadcrumbItem>
    ))}
  </Breadcrumb>
)

const ChildSelector = ({ parentId, childOptions }: ChildSelectorProps) => (
  <Dropdown
    id={"dropdown-child-selector"}
    label="Select child..."
    items={[null, ...childOptions]}
    itemToString={(child) => child?.eipId.name ?? ""}
    onChange={({ selectedItem }) => {
      selectedItem && enableChild(parentId, selectedItem.eipId)
    }}
    selectedItem={null}
    titleText={"Add a child"}
  />
)

const ChildrenDisplay = ({
  parentId,
  enabledChildren,
  updatePath,
  openChildConfigPanel,
}: ChildrenDisplayProps) => (
  <Layer>
    <DraggableList
      className="child-modal__list"
      label="Children"
      kind="on-page"
      handleDrop={(items) => reorderEnabledChildren(parentId, items)}
    >
      {enabledChildren.map((childId) => {
        const eipId = getEipId(childId)
        return (
          <DraggableListItem
            key={childId}
            id={childId}
            action={
              <>
                <Button
                  kind="ghost"
                  iconDescription="Configure"
                  hasIconOnly
                  renderIcon={SettingsEdit}
                  tooltipPosition="left"
                  onClick={() => openChildConfigPanel(childId)}
                />
                <Button
                  kind="ghost"
                  iconDescription="Delete"
                  hasIconOnly
                  renderIcon={Close}
                  tooltipPosition="left"
                  onClick={() => disableChild(parentId, childId)}
                />
              </>
            }
            onClick={() => updatePath(childId)}
          >
            <span>{eipId?.name}</span> <span>({childId})</span>
          </DraggableListItem>
        )
      })}
    </DraggableList>
  </Layer>
)

export const ChildManagementModal = ({
  rootId,
  open,
  setOpen,
  initPath,
}: ChildModalProps) => {
  const [path, setPath] = useState(initPath ?? [rootId])
  const parentId = path[path.length - 1]
  const enabledChildren = useGetEnabledChildren(parentId)

  const rootEipId = getEipId(rootId)
  const rootEipDef = rootEipId && lookupEipComponent(rootEipId)

  const childOptions = rootEipDef && getChildEipDefinition(rootEipDef, path)

  let modalContent: ReactNode
  if (childOptions && childOptions.length > 0) {
    modalContent = (
      <>
        <ChildSelector parentId={parentId} childOptions={childOptions} />
        <ChildrenDisplay
          parentId={parentId}
          enabledChildren={enabledChildren}
          updatePath={(childId) => setPath((path) => [...path, childId])}
          openChildConfigPanel={(childId) => {
            updateSelectedChildNode([...path, childId])
            setOpen(false)
          }}
        />
      </>
    )
  } else {
    modalContent = (
      <p className="child-modal__list-placeholder">No available children</p>
    )
  }

  // TODO: Capture browser "back" and "forward" clicks and apply to child path navigation
  return createPortal(
    <Modal
      className="child-modal"
      open={open}
      onRequestClose={() => setOpen(false)}
      modalHeading="Configure enabled children"
      passiveModal
      size="md"
    >
      <Stack orientation="vertical" gap={7}>
        <ChildPathBreadcrumb
          path={path}
          navigatePath={(idx: number) =>
            setPath((prev) => prev.slice(0, idx + 1))
          }
        />
        {modalContent}
      </Stack>
    </Modal>,
    document.body
  )
}
