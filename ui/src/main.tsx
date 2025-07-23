import { Theme } from "@carbon/react"
import React from "react"
import { DndProvider } from "react-dnd"
import { HTML5Backend } from "react-dnd-html5-backend"
import ReactDOM from "react-dom/client"
import { ReactFlowProvider } from "@xyflow/react"
import App from "./App.tsx"

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <DndProvider backend={HTML5Backend}>
      <ReactFlowProvider>
        <Theme theme="white">
          <App />
        </Theme>
      </ReactFlowProvider>
    </DndProvider>
  </React.StrictMode>
)
