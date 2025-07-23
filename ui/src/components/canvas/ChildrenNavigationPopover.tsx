import {
  Button,
  Popover,
  PopoverContent,
  TreeNode,
  TreeNodeProps,
  TreeView,
} from "@carbon/react"
import { ParentChild } from "@carbon/react/icons"
import { ReactElement, useState } from "react"
import { useNodeId } from "@xyflow/react"
import { updateSelectedChildNode } from "../../singletons/store/appActions"
import {
  getEipId,
  getEnabledChildrenView,
  getSelectedChildNode,
} from "../../singletons/store/storeViews"

const renderTree = (idPath: string[]): ReactElement<TreeNodeProps> => {
  const id = idPath[idPath.length - 1]
  const children = getEnabledChildrenView(id)

  return (
    <TreeNode
      className={"child-tree-view__node"}
      key={id}
      id={id}
      label={getEipId(id)?.name}
      onSelect={() => updateSelectedChildNode(idPath)}
    >
      {children && children.length > 0
        ? children.map((childId) => renderTree([...idPath, childId]))
        : null}
    </TreeNode>
  )
}

const ChildTree = () => {
  const rootId = useNodeId()

  if (!rootId) {
    return null
  }

  return (
    <div className="nodrag">
      <TreeView
        className={"child-tree-view"}
        label={"children-menu"}
        hideLabel
        size="xs"
      >
        {renderTree([rootId]).props.children}
      </TreeView>
    </div>
  )
}

export const ChildrenNavigationPopover = () => {
  const [open, setOpen] = useState(false)

  return (
    <Popover
      className="eip-children-popover nowheel"
      highContrast
      open={open}
      // Keep popover open when interacting with node config side panel
      onRequestClose={() => !getSelectedChildNode() && setOpen(false)}
      onKeyDown={(ev: React.KeyboardEvent) =>
        ev.key === "Escape" && setOpen(false)
      }
    >
      <Button
        className="eip-children-popover__button"
        hasIconOnly
        iconDescription="children"
        kind="primary"
        renderIcon={ParentChild}
        size="sm"
        tooltipPosition="left"
        onClick={() => setOpen(!open)}
      />
      <PopoverContent>
        <ChildTree />
      </PopoverContent>
    </Popover>
  )
}
