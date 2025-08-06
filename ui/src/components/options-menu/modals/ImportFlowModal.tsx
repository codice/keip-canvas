import { Modal } from "@carbon/react"
import { InlineLoadingStatus } from "@carbon/react/lib/components/InlineLoading/InlineLoading"
import { useState } from "react"
import { createPortal } from "react-dom"
import { importFlowFromJson } from "../../../singletons/store/appActions"
import { ModalCodeEditor } from "../../editor/ModalCodeEditor"

interface ImportFlowModalProps {
  open: boolean
  setOpen: (open: boolean) => void
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
      <ModalCodeEditor
        content={content}
        setContent={updateContent}
        language="json"
      />
    </Modal>,
    document.body
  )
}
