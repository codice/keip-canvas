import { CopyButton, InlineLoading } from "@carbon/react"
import hljs from "highlight.js/lib/core"
import xml from "highlight.js/lib/languages/xml"
import { useEffect, useState } from "react"
import Editor from "react-simple-code-editor"
import { useEipFlow } from "../../../singletons/store/diagramToEipFlow"

// highlight.js theme
import "highlight.js/styles/intellij-light.css"
import { fetchXmlTranslation } from "../../endpoints/translation/translateFlowToXml"

const UNMOUNT_SIGNAL = "unmount"

hljs.registerLanguage("xml", xml)

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
