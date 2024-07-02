import ReactFlow, {
  Background,
  BackgroundVariant,
  ControlButton,
  Controls,
  ReactFlowInstance,
  useReactFlow,
} from "reactflow"

import { useAppActions, useFlowStore, useGetLayout } from "../../singletons/store"
import "reactflow/dist/base.css"
import {
  TrashCan,
  ArrowsHorizontal,
  ArrowsVertical,
  Maximize,
} from "@carbon/icons-react"
import { ErrorBoundary } from "@carbon/react"
import { DropTargetMonitor, useDrop } from "react-dnd"
import { NativeTypes } from "react-dnd-html5-backend"
import { EipId } from "../../api/id"
import { DragTypes } from "../draggable-panel/dragTypes"
import { EipNode } from "./EipNode"
import { useEffect } from "react"

const FLOW_ERROR_MESSAGE =
  "Failed to load the canvas - the stored flow is malformed. Clearing the flow from the state store."

interface FileDrop {
  files: File[]
}

type DropType = EipId | FileDrop

interface ErrorHandlerProps {
  message: string
  callback: () => void
}

const ErrorHandler = ({ message, callback }: ErrorHandlerProps) => {
  console.error(message)
  callback()
  return null
}

const acceptDroppedFile = (file: File, importFlow: (json: string) => void) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    e.target && importFlow(e.target.result as string)
  }
  reader.readAsText(file)
}

const getDropPosition = (
  monitor: DropTargetMonitor,
  reactFlowInstance: ReactFlowInstance
) => {
  let offset = monitor.getClientOffset()
  offset = offset ?? { x: 0, y: 0 }
  return reactFlowInstance.screenToFlowPosition(offset)
}

const nodeTypes = {
  eipNode: EipNode,
}

const FlowCanvas = () => {
  const reactFlowInstance = useReactFlow()
  const flowStore = useFlowStore()
  const layout = useGetLayout()
  const {
    createDroppedNode,
    clearSelectedChildNode,
    clearFlow,
    importFlowFromJson,
    updateLayoutOrientation,
    updateLayoutDensity,
  } = useAppActions()

  useEffect(() => {
    reactFlowInstance.fitView()
  }, [layout, reactFlowInstance])

  const [, drop] = useDrop(
    () => ({
      accept: [DragTypes.FLOWNODE, NativeTypes.FILE],
      drop: (item: DropType, monitor) => {
        if ("namespace" in item) {
          // Dropping a FLOWNODE creates a new node in the flow.
          const pos = getDropPosition(monitor, reactFlowInstance)
          createDroppedNode(item, pos)
        } else if ("files" in item) {
          // Dropping a JSON file imports it as a flow.
          acceptDroppedFile(item.files[0], importFlowFromJson)
        } else {
          console.warn("unknown drop type: ", item)
        }
      },
      canDrop: (item: DropType) => {
        if ("files" in item) {
          return (
            item.files.length == 1 && item.files[0].type == "application/json"
          )
        }
        return true
      },
    }),
    [reactFlowInstance]
  )

  // TODO: See if there is a better way to select and clear child nodes,
  // to avoid having to clear the selection in multiple components.

  // TODO: ErrorHandler clears the flow on-error in order to recover in the case of a
  // malformed flow import. Consider less destructive options.

  return (
    <div className="canvas" ref={drop}>
      <ErrorBoundary
        fallback={
          <ErrorHandler message={FLOW_ERROR_MESSAGE} callback={clearFlow} />
        }
      >
        <ReactFlow
          nodes={flowStore.nodes}
          edges={flowStore.edges}
          nodeTypes={nodeTypes}
          onNodesChange={flowStore.onNodesChange}
          onEdgesChange={flowStore.onEdgesChange}
          onConnect={flowStore.onConnect}
          onPaneClick={() => clearSelectedChildNode()}
          fitView
        >
          <Controls style={{ bottom: "50px" }}>
            <ControlButton
              title="horizontal layout"
              onClick={() => updateLayoutOrientation("horizontal")}
            >
              <ArrowsHorizontal />
            </ControlButton>
            <ControlButton
              title="vertical layout"
              onClick={() => updateLayoutOrientation("vertical")}
            >
              <ArrowsVertical />
            </ControlButton>
            <ControlButton title="change density" onClick={updateLayoutDensity}>
              <Maximize />
            </ControlButton>
          </Controls>

          <Controls
            position="bottom-left"
            showFitView={false}
            showInteractive={false}
            showZoom={false}
          >
            <ControlButton title="clear" onClick={clearFlow}>
              <TrashCan />
            </ControlButton>
          </Controls>

          {/* <MiniMap /> */}
          <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
        </ReactFlow>
      </ErrorBoundary>
    </div>
  )
}

export default FlowCanvas
