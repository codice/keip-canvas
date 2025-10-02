import { useEffect, useState } from "react"
import { FLOW_TRANSLATOR_BASE_URL } from "../../../singletons/externalEndpoints"
import fetchWithTimeout from "../../../utils/fetch/fetchWithTimeout"

const statusEndpoint = `${FLOW_TRANSLATOR_BASE_URL}/actuator/health`

const logTranslationServerStatus = (available: boolean) => {
  if (available) {
    console.log(
      `Enable Flow XML translation: A flow translator is available at ${FLOW_TRANSLATOR_BASE_URL}`
    )
  } else {
    console.log(
      `Disable Flow XML translation: Could not connect to a flow translator at ${FLOW_TRANSLATOR_BASE_URL}`
    )
  }
}

const useTranslationServerStatus = () => {
  const [isAvailable, setIsAvailable] = useState(false)

  useEffect(() => {
    const abortCtrl = new AbortController()
    const status = fetchWithTimeout(statusEndpoint, { abortCtrl })
      .then((r) => r.ok)
      .catch(() => false)

    void (async () => {
      const resolved = await status
      logTranslationServerStatus(resolved)
      setIsAvailable(resolved)
    })()

    return () => {
      abortCtrl.abort("Component unmounted")
    }
  }, [])

  return isAvailable
}

export default useTranslationServerStatus
