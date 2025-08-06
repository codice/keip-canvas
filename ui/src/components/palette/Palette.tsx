import { useState } from "react"
import { Panel, PanelGroup, PanelResizeHandle } from "react-resizable-panels"
import { CustomEntityPanel } from "./CustomEntityPanel"
import { EipComponentPanel } from "./EipComponentPanel"

// TODO: Make node chooser panel collapsable
const Palette = () => {
  const [isEntityPanelCollapsed, setEntityPanelCollapsed] = useState(true)

  const entityPanel = (
    <CustomEntityPanel
      isCollapsed={isEntityPanelCollapsed}
      setCollapsed={setEntityPanelCollapsed}
    />
  )

  const wrappedEntityPanel = (
    <Panel
      id="entities"
      defaultSize={20}
      minSize={10}
      maxSize={50}
      order={2}
      style={{ overflowY: "auto" }}
    >
      {entityPanel}
    </Panel>
  )

  return (
    <PanelGroup
      autoSaveId="keip-node-palette"
      className="node-palette"
      direction="vertical"
    >
      <Panel
        id="eipComponents"
        minSize={50}
        order={1}
        style={{ overflowY: "auto" }}
      >
        <EipComponentPanel />
      </Panel>
      <PanelResizeHandle
        className="resizable-panel-divider"
        disabled={isEntityPanelCollapsed}
      />

      {isEntityPanelCollapsed ? entityPanel : wrappedEntityPanel}
    </PanelGroup>
  )
}

export default Palette
