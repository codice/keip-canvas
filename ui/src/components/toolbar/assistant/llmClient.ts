import { Ollama } from "@langchain/ollama"
import { BuiltInEdge, Node } from "@xyflow/react"
import { EipId } from "../../../api/generated/eipFlow"
import { KEIP_ASSISTANT_OLLAMA_URL } from "../../../singletons/externalEndpoints"
import { EipConfig, SerializedFlow } from "../../../singletons/store/api"
import { EXPORTED_FLOW_VERSION } from "../../../singletons/store/appStore"

import { CustomNodeType } from "../../../api/flow"
import {
  getEdgesView,
  getEipId,
  getLayoutView,
  getNodesView,
} from "../../../singletons/store/storeViews"
import fetchWithTimeout from "../../../utils/fetch/fetchWithTimeout"
import { newFlowLayout } from "../../layout/layouting"
import { fuzzyEipMatch } from "./fuzzyEipIdMatch"
import { flowCreatePrompt, flowUpdatePrompt } from "./prompt"

// react flow requires using a type rather than an interface for EdgeData
// eslint-disable-next-line @typescript-eslint/consistent-type-definitions
type GenNodeData = {
  eipId: EipId
  label?: string
}

interface ModelFlowResponse {
  nodes: Node<GenNodeData>[]
  edges: BuiltInEdge[]
  eipConfigs: Record<string, EipConfig>
}

interface PromptResponse {
  raw: string
  success: boolean
  cause?: Error | "aborted"
  processedFlow?: SerializedFlow
}

class LlmClient {
  private llm
  private abortCtrl
  public serverBaseUrl = KEIP_ASSISTANT_OLLAMA_URL

  constructor() {
    this.llm = new Ollama({
      baseUrl: this.serverBaseUrl,
      maxRetries: 3,
      model: "keip-assistant",
      format: "json",
      temperature: 0,
    })

    this.abortCtrl = new AbortController()
  }

  public async prompt(
    userInput: string,
    streamCallback: (data: string) => void
  ): Promise<PromptResponse> {
    let rawResponse = ""
    try {
      const prompt = await this.generatePrompt()
      console.log("user input: ", userInput)
      const chain = prompt.pipe(this.llm)
      const responseStream = await chain.stream({
        userInput: userInput,
      })

      for await (const chunk of responseStream) {
        if (this.abortCtrl.signal.aborted) {
          this.abortCtrl = new AbortController()
          return { raw: rawResponse, success: false, cause: "aborted" }
        }

        rawResponse += chunk
        streamCallback(rawResponse)
      }

      return {
        raw: rawResponse,
        processedFlow: this.parseResponse(rawResponse),
        success: true,
      }
    } catch (err) {
      console.error(err)
      return {
        raw: rawResponse,
        success: false,
        cause: err as Error,
      }
    }
  }

  public abortPrompt(): void {
    this.abortCtrl.abort()
  }

  public ping(): { success: Promise<boolean>; abort: () => void } {
    const abortCtrl = new AbortController()
    const success = fetchWithTimeout(this.serverBaseUrl, { abortCtrl })
      .then((res) => res.ok)
      .catch(() => false)
    return { success, abort: () => abortCtrl.abort() }
  }

  private async generatePrompt() {
    const nodes = getNodesView()
    if (nodes && nodes.length > 0) {
      const currFlow = JSON.stringify({
        nodes: nodes.map((n) => ({
          id: n.id,
          type: n.type,
          data: { eipId: getEipId(n.id) },
        })),
        edges: getEdgesView(),
      })
      const prompt = flowUpdatePrompt
      return await prompt.partial({
        existingFlowJson: currFlow,
      })
    } else {
      return flowCreatePrompt
    }
  }

  // TODO: Use Langchain custom output parser
  private parseResponse(jsonResponse: string) {
    const response = JSON.parse(jsonResponse) as ModelFlowResponse
    if (!response.nodes) {
      throw new Error("No nodes provided in model response: " + jsonResponse)
    }

    if (!response.edges) {
      if (response.nodes.length === 1) {
        response.edges = []
      } else {
        throw new Error("No edges provided in model response: " + jsonResponse)
      }
    }

    response.eipConfigs = {}
    collectEipIds(response)
    const eipFlow = convertToSerializedFlow(response)

    const layout = getLayoutView()
    eipFlow.nodes = newFlowLayout(eipFlow.nodes, eipFlow.edges, layout)

    return eipFlow
  }
}

const convertToSerializedFlow = (
  response: ModelFlowResponse
): SerializedFlow => ({
  nodes: response.nodes.map((node) => ({
    ...node,
    type: CustomNodeType.EipNode,
    data: { label: node?.data?.label },
  })),
  edges: response.edges,
  eipConfigs: response.eipConfigs,
  version: EXPORTED_FLOW_VERSION,
})

const collectEipIds = (response: ModelFlowResponse) =>
  response.nodes.forEach((node) => {
    const eipId = fuzzyEipMatch(node.data.eipId)
    response.eipConfigs[node.id] = { eipId, attributes: {}, children: [] }
  })

export const llmClientInstance = new LlmClient()
