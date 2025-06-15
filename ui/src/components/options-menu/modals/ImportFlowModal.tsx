import { Modal } from "@carbon/react"
import { InlineLoadingStatus } from "carbon-components-react"
import hljs from "highlight.js/lib/core"
import json from "highlight.js/lib/languages/json"
import { useState } from "react"
import { createPortal } from "react-dom"
import Editor from "react-simple-code-editor"
import { importFlowFromJson } from "../../../singletons/store/appActions"

hljs.registerLanguage("json", json)

interface ImportFlowModalProps {
  open: boolean
  setOpen: (open: boolean) => void
}

interface JsonEditorProps {
  content: string
  setContent: (content: string) => void
}

const getLoadingDescription = (status: InlineLoadingStatus) => {
  switch (status) {
    case "active":
      return "importing JSON"
    case "error":
      return "Invalid Flow JSON"
    case "inactive":
    case "finished":
      return ""
  }
}

const FlowJsonEditor = ({ content, setContent }: JsonEditorProps) => {
  return (
    <div className="options-modal__editor">
      <Editor
        value={content}
        onValueChange={(code) => setContent(code)}
        highlight={(code) => hljs.highlight(code, { language: "json" }).value}
        padding={16}
        textareaClassName="options-modal__editor-textarea"
      />
    </div>
  )
}

export const ImportFlowModal = ({ open, setOpen }: ImportFlowModalProps) => {
  const [loadingStatus, setLoadingStatus] =
    useState<InlineLoadingStatus>("inactive")
  const [content, setContent] = useState("")

  const resetAndCloseModal = () => {
    setOpen(false)
    setLoadingStatus("inactive")
    setContent("")
  }

  const doImport = () => {
    setLoadingStatus("active")
    try {
      importFlowFromJson(content)
      resetAndCloseModal()
    } catch {
      setLoadingStatus("error")
      return
    }
  }

  const updateContent = (content: string) => {
    loadingStatus !== "inactive" && setLoadingStatus("inactive")
    setContent(content)
  }

  return createPortal(
    <Modal
      open={open}
      onRequestClose={resetAndCloseModal}
      size="md"
      modalHeading="Import a Flow JSON"
      primaryButtonText="Import"
      secondaryButtonText="Cancel"
      loadingStatus={loadingStatus}
      loadingDescription={getLoadingDescription(loadingStatus)}
      onRequestSubmit={doImport}
    >
      <FlowJsonEditor content={content} setContent={updateContent} />
    </Modal>,
    document.body
  )
}
