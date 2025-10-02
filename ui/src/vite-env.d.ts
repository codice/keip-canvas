/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_FLOW_TRANSLATOR_BASE_URL: string
  readonly VITE_KEIP_ASSISTANT_DOCS_URL: string
  readonly VITE_KEIP_ASSISTANT_OLLAMA_URL: string
  readonly VITE_KEIP_K8S_CLUSTER_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
