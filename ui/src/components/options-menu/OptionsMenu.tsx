import { OverflowMenu, OverflowMenuItem } from "@carbon/react"
import { Menu } from "@carbon/react/icons"
import { useState } from "react"
import useKeipClientStatus from "../endpoints/deploy/keipClientStatusHook"
import useTranslationServerStatus from "../endpoints/translation/translatorStatusHook"
import ExportPng from "./ExportPng"
import SaveDiagram from "./SaveDiagram"
import { DeployRouteModal } from "./modals/DeployRouteModal"
import { ImportFlowModal } from "./modals/ImportFlowModal"

const OptionsMenu = () => {
  const [importFlowModalOpen, setImportFlowModalOpen] = useState(false)
  const [deployModalOpen, setDeployModalOpen] = useState(false)
  const isKeipWebhookAvailable = useKeipClientStatus()
  const isTranslationServerAvailable = useTranslationServerStatus()

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

        {/* Route deployment requires both the flow translator and Keip controller endpoints to be available */}
        <OverflowMenuItem
          itemText="Deploy Route"
          onClick={() => setDeployModalOpen(true)}
          disabled={!isKeipWebhookAvailable || !isTranslationServerAvailable}
        />
      </OverflowMenu>

      {/* Modals */}
      <ImportFlowModal
        open={importFlowModalOpen}
        setOpen={setImportFlowModalOpen}
      />
      <DeployRouteModal open={deployModalOpen} setOpen={setDeployModalOpen} />
    </>
  )
}

export default OptionsMenu
