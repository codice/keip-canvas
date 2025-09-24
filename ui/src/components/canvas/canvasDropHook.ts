import { ReactFlowInstance } from "@xyflow/react"
import "@xyflow/react/dist/style.css"
import { DropTargetMonitor, useDrop } from "react-dnd"
import { NativeTypes } from "react-dnd-html5-backend"
import { EipFlow, EipId } from "../../api/generated/eipFlow"
import { FLOW_TRANSLATOR_BASE_URL } from "../../singletons/externalEndpoints"
import { SerializedFlow } from "../../singletons/store/api"
import {
  createDroppedNode,
  importFlowFromJson,
  importFlowFromObject,
} from "../../singletons/store/appActions"
import { eipFlowToDiagram } from "../../singletons/store/eipFlowToDiagram"
import fetchWithTimeout from "../../utils/fetch/fetchWithTimeout"
import { DragTypes } from "../palette/dragTypes"

interface XmlTranslationResponse {
  data?: EipFlow
  error?: {
    message: string
    type: string
    details: object[]
  }
}

interface FileDrop {
  files: File[]
}

type DropType = EipId | FileDrop

const FileTypes = {
  Json: "application/json",
  Xml: "text/xml",
} as const

type FileType = (typeof FileTypes)[keyof typeof FileTypes]

const supportedFileTypes = new Set(Object.values(FileTypes))

const isSupportedFileType = (value: string): value is FileType => {
  return supportedFileTypes.has(value as FileType)
}

const translateXmlToFlow = async (xml: string) => {
  const response = await fetchWithTimeout(
    `${FLOW_TRANSLATOR_BASE_URL}/translation/toFlow`,
    {
      method: "POST",
      body: xml,
      headers: {
        "Content-Type": "application/xml",
      },
      timeout: 10000,
    }
  )

  const { data, error } = (await response.json()) as XmlTranslationResponse

  if (!response.ok) {
    throw new Error(JSON.stringify(error))
  }

  return data!
}

const importFile = (
  readEvent: ProgressEvent<FileReader>,
  fileType: FileType
) => {
  switch (fileType) {
    case FileTypes.Json: {
      readEvent.target && importFlowFromJson(readEvent.target.result as string)
      break
    }
    case FileTypes.Xml: {
      readEvent.target &&
        translateXmlToFlow(readEvent.target.result as string)
          .then((flow) => {
            const flowDiagram = eipFlowToDiagram(flow)
            importFlowFromObject(flowDiagram as SerializedFlow)
          })
          .catch((err: Error) =>
            console.log("Failed to import integration XML file:", err)
          )
      break
    }
  }
}

const validateDroppedFiles = (files: File[]) => {
  if (files.length !== 1) {
    console.error("Multiple file drops are not supported")
    return false
  }

  const fileType = files[0].type
  if (!isSupportedFileType(fileType)) {
    console.error(
      `${fileType} is not a supported type. Dropped file must either be an EIP Flow JSON or an Integration XML`
    )
    return false
  }

  return true
}

const acceptDroppedFile = (file: File) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      importFile(e, file.type as FileType)
    } catch (e) {
      // TODO: Display an error pop-up on failed import
      // https://github.com/codice/keip-canvas/issues/7
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

export const useCanvasDrop = (reactFlowInstance: ReactFlowInstance) => {
  const [, drop] = useDrop(
    () => ({
      accept: [DragTypes.FLOWNODE, NativeTypes.FILE],
      drop: (item: DropType, monitor) => {
        if ("namespace" in item) {
          // Dropping a FLOWNODE creates a new node in the flow.
          const pos = getDropPosition(monitor, reactFlowInstance)
          createDroppedNode(item, pos)
        } else if ("files" in item) {
          validateDroppedFiles(item.files) && acceptDroppedFile(item.files[0])
        } else {
          console.warn("unknown drop type: ", item)
        }
      },
    }),
    [reactFlowInstance]
  )

  return drop
}
