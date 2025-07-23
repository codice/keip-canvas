import { OverflowMenuItem } from "@carbon/react"
import { OverflowMenuItemProps } from "@carbon/react/lib/components/OverflowMenuItem/OverflowMenuItem"
import { layer01 } from "@carbon/themes"
import { getNodesBounds, getViewportForBounds } from "@xyflow/react"
import { toPng } from "html-to-image"
import { forwardRef } from "react"
import { clearDiagramSelections } from "../../singletons/store/appActions"
import { useGetNodes } from "../../singletons/store/getterHooks"

const SCALING_FACTOR = 1.5

const downloadImage = (dataUrl: string) => {
  const a = document.createElement("a")
  a.setAttribute("download", "flowDiagram.png")
  a.setAttribute("href", dataUrl)
  a.click()
  a.remove()
}

const ExportPng = forwardRef<HTMLElement>(
  (props: OverflowMenuItemProps, ref) => {
    const nodes = useGetNodes()

    const handleClick = () => {
      clearDiagramSelections()
      const nodeBounds = getNodesBounds(nodes)
      const imageWidth = nodeBounds.width * SCALING_FACTOR
      const imageHeight = nodeBounds.height * SCALING_FACTOR
      const transform = getViewportForBounds(
        nodeBounds,
        imageWidth,
        imageHeight,
        0.5,
        2,
        0.1
      )

      const viewport: HTMLElement = document.querySelector(
        ".react-flow__viewport"
      )!

      toPng(viewport, {
        backgroundColor: layer01,
        width: imageWidth,
        height: imageHeight,
        pixelRatio: 1,
        style: {
          width: imageWidth.toString(),
          height: imageHeight.toString(),
          transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.zoom})`,
        },
      })
        .then(downloadImage)
        .catch((err) => console.error("Failed to convert diagram to PNG", err))
    }

    return (
      <OverflowMenuItem
        {...props}
        ref={ref}
        itemText="Export image"
        onClick={handleClick}
      />
    )
  }
)

ExportPng.displayName = "ExportPng"

export default ExportPng
