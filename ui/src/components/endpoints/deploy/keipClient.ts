import { K8S_CLUSTER_URL } from "../../../singletons/externalEndpoints"
import fetchWithTimeout from "../../../utils/fetch/fetchWithTimeout"

interface Route {
  name: string
  namespace: string
  xml: string
}

interface Resource {
  name: string
  status: "created" | "deleted" | "updated" | "recreated"
}

interface RouteDeployRequest {
  routes: Route[]
}

type RouteDeployResponse = Resource[]

class KeipClient {
  public serverBaseUrl = K8S_CLUSTER_URL

  public ping(): Promise<boolean> {
    const status = fetchWithTimeout(`${this.serverBaseUrl}/status`, {
      timeout: 5000,
    })
      .then((res) => res.ok)
      .catch(() => false)
    return status
  }

  public async deploy(
    request: RouteDeployRequest
  ): Promise<RouteDeployResponse> {
    const response = await fetchWithTimeout(`${this.serverBaseUrl}/route`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      timeout: 10000,
    })

    if (!response.ok) {
      throw new Error(`Failed to deploy route: ${response.status}`)
    }

    return (await response.json()) as RouteDeployResponse
  }
}

export const keipClient = new KeipClient()
