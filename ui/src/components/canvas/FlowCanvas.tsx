import {
  ArrowsHorizontal,
  ArrowsVertical,
  Maximize,
  Redo,
  TrashCan,
  Undo,
} from "@carbon/icons-react"
import { ErrorBoundary } from "@carbon/react"
import {
  Background,
  BackgroundVariant,
  ControlButton,
  Controls,
  ReactFlow,
  useReactFlow,
} from "@xyflow/react"
import "@xyflow/react/dist/style.css"
import { KeyboardEvent, useEffect } from "react"
import { CustomNodeType, DYNAMIC_EDGE_TYPE } from "../../api/flow"
import {
  clearFlow,
  clearSelectedChildNode,
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
import DynamicEdge from "./DynamicEdge"
import { EipNode, FollowerNode } from "./EipNode"
import { useCanvasDrop } from "./canvasDropHook"

const FLOW_ERROR_MESSAGE =
  "Failed to load the canvas - the stored flow is malformed. Clearing the flow from the state store."

interface ErrorHandlerProps {
  message: string
  callback: () => void
}

const ErrorHandler = ({ message, callback }: ErrorHandlerProps) => {
  console.error(message)
  callback()
  return null
}

const nodeTypes = {
  [CustomNodeType.EipNode]: EipNode,
  [CustomNodeType.FollowerNode]: FollowerNode,
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
  const drop = useCanvasDrop(reactFlowInstance)

  useEffect(() => {
    reactFlowInstance
      .fitView()
      .catch((e) => console.warn("failed to call fitView", e))
  }, [layout, reactFlowInstance])

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
              {/* TODO: style this button as a dangerous action */}
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
