import {
  ArrowsHorizontal,
  ArrowsVertical,
  Maximize,
  Redo,
  TrashCan,
  Undo,
} from "@carbon/icons-react"
import { ErrorBoundary } from "@carbon/react"
import { KeyboardEvent, useEffect } from "react"
import { DropTargetMonitor, useDrop } from "react-dnd"
import { NativeTypes } from "react-dnd-html5-backend"
import {
  Background,
  BackgroundVariant,
  ControlButton,
  Controls,
  ReactFlow,
  ReactFlowInstance,
  useReactFlow,
} from "reactflow"
import "reactflow/dist/base.css"
import { DYNAMIC_EDGE_TYPE, EIP_NODE_TYPE } from "../../api/flow"
import { EipId } from "../../api/generated/eipFlow"
import {
  clearFlow,
  clearSelectedChildNode,
  createDroppedNode,
  importFlowFromJson,
  toggleLayoutDensity,
  updateLayoutOrientation,
} from "../../singletons/store/appActions"
import {
  useFlowStore,
  useGetLayout,
  useUndoRedo,
} from "../../singletons/store/getterHooks"
import {
  onConnect,
  onEdgesChange,
  onNodesChange,
} from "../../singletons/store/reactFlowActions"
import { DragTypes } from "../draggable-panel/dragTypes"
import DynamicEdge from "./DynamicEdge"
import { EipNode } from "./EipNode"

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
    try {
      e.target && importFlow(e.target.result as string)
    } catch (e) {
      // TODO: Display an error pop-up on failed import
      // https://github.com/OctoConsulting/keip-canvas/issues/7
      console.error((e as Error).message)
    }
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
  [EIP_NODE_TYPE]: EipNode,
}

const edgeTypes = {
  [DYNAMIC_EDGE_TYPE]: DynamicEdge,
}

const onUndoRedoKeyDown = (
  event: KeyboardEvent,
  undo: () => void,
  redo: () => void
) => {
  if (
    (event.ctrlKey && event.shiftKey && event.key === "Z") ||
    (event.metaKey && event.shiftKey && event.key === "Z") ||
    (event.metaKey && event.key === "y") ||
    (event.ctrlKey && event.key === "y")
  ) {
    event.preventDefault()
    redo()
  } else if (
    (event.ctrlKey && event.key === "z") ||
    (event.metaKey && event.key === "z")
  ) {
    event.preventDefault()
    undo()
  }
}

const FlowCanvas = () => {
  const reactFlowInstance = useReactFlow()
  const flowStore = useFlowStore()
  const layout = useGetLayout()
  const { undo, redo } = useUndoRedo()

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
          edgeTypes={edgeTypes}
          tabIndex={0}
          onKeyDown={(e) =>
            onUndoRedoKeyDown(
              e,
              () => undo(),
              () => redo()
            )
          }
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onPaneClick={() => clearSelectedChildNode()}
          fitView
        >
          <Controls style={{ bottom: "235px" }} />

          <Controls
            style={{ bottom: "130px" }}
            showFitView={false}
            showInteractive={false}
            showZoom={false}
          >
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
            <ControlButton title="change density" onClick={toggleLayoutDensity}>
              <Maximize />
            </ControlButton>
          </Controls>

          <Controls
            style={{ bottom: "50px" }}
            showFitView={false}
            showInteractive={false}
            showZoom={false}
          >
            <ControlButton title="undo" onClick={() => undo()}>
              <Undo />
            </ControlButton>
            <ControlButton title="redo" onClick={() => redo()}>
              <Redo />
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
