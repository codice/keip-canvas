import { OverflowMenu, OverflowMenuItem } from "@carbon/react"
import { Menu } from "@carbon/react/icons"
import { useState } from "react"
import ExportPng from "./ExportPng"
import SaveDiagram from "./SaveDiagram"
import { ImportFlowModal } from "./modals/ImportFlowModal"

const OptionsMenu = () => {
  const [importFlowModalOpen, setImportFlowModalOpen] = useState(false)

  return (
    <>
      <OverflowMenu
        className="options-menu"
        aria-label="options-menu"
        iconDescription="options"
        align="bottom-end"
        renderIcon={() => <Menu size={24} />}
        size="lg"
        flipped
      >
        {/* OverflowMenu does not play nice when OverflowMenuItems are not direct children. Custom components will need to use forwardRef to avoid errors. */}
        <SaveDiagram />
        <ExportPng />
        <OverflowMenuItem
          itemText="Import Flow JSON"
          onClick={() => setImportFlowModalOpen(true)}
        />
      </OverflowMenu>

      {/* Modals */}
      <ImportFlowModal
        open={importFlowModalOpen}
        setOpen={setImportFlowModalOpen}
      />
    </>
  )
}

export default OptionsMenu
