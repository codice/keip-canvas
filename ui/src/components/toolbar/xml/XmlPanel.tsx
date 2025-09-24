import { CopyButton, InlineLoading } from "@carbon/react"
import hljs from "highlight.js/lib/core"
import xml from "highlight.js/lib/languages/xml"
import { useEffect, useState } from "react"
import Editor from "react-simple-code-editor"
import { EipFlow } from "../../../api/generated/eipFlow"
import { useEipFlow } from "../../../singletons/store/diagramToEipFlow"

// highlight.js theme
import "highlight.js/styles/intellij-light.css"
import { FLOW_TRANSLATOR_BASE_URL } from "../../../singletons/externalEndpoints"
import fetchWithTimeout from "../../../utils/fetch/fetchWithTimeout"

const UNMOUNT_SIGNAL = "unmount"

hljs.registerLanguage("xml", xml)

interface FlowTranslationResponse {
  data?: string
  error?: {
    message: string
    type: string
    details: object[]
  }
}

type InlineLoadingProps = React.ComponentProps<typeof InlineLoading>

const getLoadingStatus = (
  isLoading: boolean,
  isError: boolean
): { status: InlineLoadingProps["status"]; description: string } => {
  if (isLoading) {
    return { status: "active", description: "loading" }
  }
  return isError
    ? { status: "error", description: "error" }
    : { status: "finished", description: "synced" }
}

// TODO: Add client-side caching (might make sense to use a data fetching library)
const fetchXmlTranslation = async (
  flow: EipFlow,
  abortCtrl: AbortController
) => {
  const queryStr = new URLSearchParams({ prettyPrint: "true" }).toString()
  const response = await fetchWithTimeout(
    `${FLOW_TRANSLATOR_BASE_URL}/translation/toSpringXml?` + queryStr,
    {
      method: "POST",
      body: JSON.stringify(flow),
      headers: {
        "Content-Type": "application/json",
      },
      timeout: 20000,
      abortCtrl,
    }
  )

  const { data, error } = (await response.json()) as FlowTranslationResponse

  if (!response.ok) {
    console.error("Failed to convert diagram to XML:", error)
    throw new Error(JSON.stringify(error))
  }

  return data!
}

const XmlPanel = () => {
  const [content, setContent] = useState("")
  const [isLoading, setLoading] = useState(false)
  const [isError, setError] = useState(false)
  const eipFlow = useEipFlow()

  useEffect(() => {
    const abortCtrl = new AbortController()
    setLoading(true)
    setError(false)
    fetchXmlTranslation(eipFlow, abortCtrl)
      .then((data) => setContent(data))
      .catch((err: Error) => {
        if (abortCtrl.signal.reason !== UNMOUNT_SIGNAL) {
          setError(true)
          err.message && setContent(`Error:\n\n${err.message}`)
        }
      })
      .finally(() => setLoading(false))

    return () => {
      abortCtrl.abort(UNMOUNT_SIGNAL)
    }
  }, [eipFlow])

  const loadingStatus = getLoadingStatus(isLoading, isError)

  return (
    <>
      <div className="xml-editor-container">
        <div className="xml-editor-loading-wrapper">
          <div>
            <InlineLoading
              style={{ paddingLeft: "0.75rem" }}
              status={loadingStatus.status}
              iconDescription={loadingStatus.description}
            />
            <CopyButton
              align="left"
              onClick={() => void navigator.clipboard.writeText(content)}
            />
          </div>
        </div>
        <Editor
          value={content}
          onValueChange={(code) => code}
          readOnly
          highlight={(code) => hljs.highlight(code, { language: "xml" }).value}
          padding={16}
        />
      </div>
    </>
  )
}

export default XmlPanel
