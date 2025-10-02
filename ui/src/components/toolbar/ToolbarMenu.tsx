import { IconButton, TableToolbar, TableToolbarContent } from "@carbon/react"
import { MachineLearning, Xml } from "@carbon/react/icons"
import { useState } from "react"
import { KEIP_ASSISTANT_DOCS_URL } from "../../singletons/externalEndpoints"
import AssistantChatPanel from "./assistant/AssistantChatPanel"
import { useLlmServerStatus } from "./assistant/llmStatusHook"
import XmlPanel from "./xml/XmlPanel"
import useTranslationServerStatus from "../endpoints/translation/translatorStatusHook"

interface PanelButtonProps {
  name: string
  icon: React.ReactNode
  disabled: boolean
  selected: boolean
  handleClick: () => void
  helpLink?: string
}

type PanelKeys = "keipAssistant" | "xml"

interface PanelRef {
  icon: React.ReactNode
  panel: React.ReactNode
  enabled?: boolean
  helpLink?: string
}

type Panels = Record<PanelKeys, PanelRef>

const panels: Panels = {
  keipAssistant: {
    icon: <MachineLearning />,
    panel: <AssistantChatPanel />,
    enabled: false,
    helpLink: KEIP_ASSISTANT_DOCS_URL,
  },
  xml: {
    icon: <Xml />,
    panel: <XmlPanel />,
    enabled: false,
  },
}

const PanelButton = ({
  name,
  icon,
  disabled,
  selected,
  handleClick,
  helpLink,
}: PanelButtonProps) => {
  const button = (
    <IconButton
      className={
        selected ? "toolbar-button toolbar-button-selected" : "toolbar-button"
      }
      label={name}
      align="left"
      kind="secondary"
      size="lg"
      onClick={handleClick}
      disabled={disabled}
    >
      {icon}
    </IconButton>
  )

  if (helpLink && disabled) {
    return (
      <a
        className="help-link"
        href={KEIP_ASSISTANT_DOCS_URL}
        target="_blank"
        rel="noopener noreferrer"
      >
        {button}
      </a>
    )
  }

  return button
}

const ToolbarMenu = () => {
  const [openPanel, setOpenPanel] = useState<"" | PanelKeys>("")
  const isLlmServerAvailable = useLlmServerStatus()
  const isTranslationServerAvailable = useTranslationServerStatus()

  panels.keipAssistant.enabled = isLlmServerAvailable
  panels.xml.enabled = isTranslationServerAvailable

  const handlePanelButtonClick = (name: PanelKeys) => () => {
    openPanel === name ? setOpenPanel("") : setOpenPanel(name)
  }

  const display = openPanel ? { height: "30vh" } : { height: "2rem" }

  // TODO: Can probably ditch Carbon's Toolbar
  return (
    <div className="main-toolbar-container" style={display}>
      <TableToolbar size="sm">
        <TableToolbarContent className="toolbar-content">
          {Object.entries(panels).map(([panelName, val]) => (
            <PanelButton
              key={panelName}
              name={panelName}
              icon={val.icon}
              disabled={!val.enabled}
              selected={panelName === openPanel}
              handleClick={handlePanelButtonClick(panelName as PanelKeys)}
              helpLink={val.helpLink}
            />
          ))}
        </TableToolbarContent>
      </TableToolbar>

      {openPanel && panels[openPanel].panel}
    </div>
  )
}

export default ToolbarMenu
