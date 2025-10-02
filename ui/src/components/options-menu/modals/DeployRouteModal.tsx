import { Modal, Stack, TextInput } from "@carbon/react"
import { InlineLoadingStatus } from "@carbon/react/lib/components/InlineLoading/InlineLoading"
import { useState } from "react"
import { createPortal } from "react-dom"
import { getEipFlow } from "../../../singletons/store/storeViews"
import { keipClient } from "../../endpoints/deploy/keipClient"
import { fetchXmlTranslation } from "../../endpoints/translation/translateFlowToXml"

interface DeployRouteModalProps {
  open: boolean
  setOpen: (open: boolean) => void
}

const getLoadingDescription = (status: InlineLoadingStatus) => {
  switch (status) {
    case "active":
      return "Deploying Route"
    case "error":
      return "Deploy Failed"
    case "finished":
      return "Deployed"
    case "inactive":
      return ""
  }
}

// TODO: Display status for deployed routes once Keip controller endpoint adds support for status checks.
// TODO: Store deployed route name and namespace in the AppStore to survive refreshes.
export const DeployRouteModal = ({ open, setOpen }: DeployRouteModalProps) => {
  const [loadingStatus, setLoadingStatus] =
    useState<InlineLoadingStatus>("inactive")

  const [name, setName] = useState("")
  const [namespace, setNamespace] = useState("default")

  const onClose = () => {
    setOpen(false)
    loadingStatus !== "active" && setLoadingStatus("inactive")
  }

  const handleDeploy = () => {
    setLoadingStatus("active")
    deployDiagram()
      .then(() => setLoadingStatus("finished"))
      .catch((err) => {
        console.error(err)
        setLoadingStatus("error")
      })
  }

  const deployDiagram = async () => {
    const flow = getEipFlow()

    if (flow.nodes?.length === 0) {
      throw new Error("Failed to deploy - diagram is empty")
    }

    const xml = await fetchXmlTranslation(flow, new AbortController())

    if (!xml) {
      throw new Error("Failed to deploy - translated XML is empty")
    }

    const request = {
      routes: [
        {
          name,
          namespace,
          xml,
        },
      ],
    }

    await keipClient.deploy(request)
  }

  return createPortal(
    <Modal
      loadingDescription={getLoadingDescription(loadingStatus)}
      loadingStatus={loadingStatus}
      modalHeading="Deploy Routes"
      onRequestClose={onClose}
      onRequestSubmit={handleDeploy}
      open={open}
      primaryButtonText="Deploy"
      secondaryButtonText="Cancel"
    >
      <Stack gap={6}>
        <TextInput
          id="name-input"
          labelText="Name"
          placeholder="Route name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <TextInput
          id="namespace-input"
          labelText="Namespace"
          value={namespace}
          onChange={(e) => setNamespace(e.target.value)}
        />
      </Stack>
    </Modal>,
    document.body
  )
}
