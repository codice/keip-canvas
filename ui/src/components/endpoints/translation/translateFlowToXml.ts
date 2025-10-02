import { EipFlow } from "../../../api/generated/eipFlow"

// highlight.js theme
import "highlight.js/styles/intellij-light.css"
import { FLOW_TRANSLATOR_BASE_URL } from "../../../singletons/externalEndpoints"
import fetchWithTimeout from "../../../utils/fetch/fetchWithTimeout"

interface FlowTranslationResponse {
  data?: string
  error?: {
    message: string
    type: string
    details: object[]
  }
}

// TODO: Add client-side caching (might make sense to use a data fetching library)
export const fetchXmlTranslation = async (
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
