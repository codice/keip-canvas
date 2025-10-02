import { useEffect, useState } from "react"
import { keipClient } from "./keipClient"

const logKeipClientStatus = (available: boolean) => {
  if (available) {
    console.log(`A KEIP Controller is available at ${keipClient.serverBaseUrl}`)
  } else {
    console.log(
      `Could not connect to a KEIP Controller at ${keipClient.serverBaseUrl}`
    )
  }
}

const useKeipClientStatus = () => {
  const [isAvailable, setIsAvailable] = useState(false)

  useEffect(() => {
    void (async () => {
      const status = await keipClient.ping()
      logKeipClientStatus(status)
      setIsAvailable(status)
    })()
  }, [])

  return isAvailable
}

export default useKeipClientStatus
