import { BaseEdge, EdgeProps, getBezierPath } from "@xyflow/react"
import { type DynamicEdge } from "../../api/flow"

const DynamicEdge = ({
  id,
  sourceX,
  sourceY,
  sourcePosition,
  targetX,
  targetY,
  targetPosition,
}: EdgeProps<DynamicEdge>) => {
  const [path] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  })

  return <BaseEdge id={id} path={path} />
}

export default DynamicEdge
