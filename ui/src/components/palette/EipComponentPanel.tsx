import {
  Accordion,
  AccordionItem,
  Search,
  SideNavMenuItem,
} from "@carbon/react"

import { useEffect, useState } from "react"
import { DragPreviewImage, useDrag } from "react-dnd"
import { EipComponent } from "../../api/generated/eipComponentDef"
import { EipId } from "../../api/generated/eipFlow"
import { EIP_SCHEMA } from "../../singletons/eipDefinitions"
import getIconUrl from "../../singletons/eipIconCatalog"
import { useNodeCount } from "../../singletons/store/getterHooks"
import { getNamespaceAlias, toTitleCase } from "../../utils/titleTransform"
import { DragTypes } from "./dragTypes"

interface Namespace {
  name: string
  components: EipComponent[]
}

interface EipItemProps {
  eipId: EipId
}

interface EipBlockCollectionProps {
  namespace: Namespace
  expandedNamespace: string
  handleExpand: (isExpanded: boolean) => void
  allExpanded: boolean
}

// TODO: Show description docs on hover
const EipItem = ({ eipId }: EipItemProps) => {
  const [{ isDragging }, drag, preview] = useDrag(() => ({
    type: DragTypes.FLOWNODE,
    item: eipId,
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }))

  const iconUrl = getIconUrl(eipId)!
  const opacity = isDragging ? 0.5 : 1

  return (
    <SideNavMenuItem className="eip-menu-item" ref={drag} style={{ opacity }}>
      <div className="eip-item-wrapper">
        <img className="eip-item-image" src={iconUrl} />
        <span>{toTitleCase(eipId.name)}</span>
        <DragPreviewImage connect={preview} src={iconUrl} />
      </div>
    </SideNavMenuItem>
  )
}

const EipNamespaceCollection = ({
  namespace,
  expandedNamespace,
  handleExpand,
  allExpanded,
}: EipBlockCollectionProps) => {
  const eipItems = namespace.components.map((c) => (
    <EipItem key={c.eipId.name} eipId={c.eipId} />
  ))

  return (
    <AccordionItem
      title={toTitleCase(getNamespaceAlias(namespace.name))}
      open={allExpanded || expandedNamespace === namespace.name}
      onHeadingClick={({ isOpen }) => handleExpand(isOpen)}
    >
      <ul>{eipItems}</ul>
    </AccordionItem>
  )
}

const namespacesToDisplay = (
  entries: [string, EipComponent[]][],
  searchTerm: string
): Namespace[] => {
  const withFilteredComponents = entries.map(([namespace, components]) => {
    const filtered = searchTerm
      ? components.filter((c) =>
          c.eipId.name.toLowerCase().includes(searchTerm)
        )
      : components

    filtered.sort((a, b) => a.eipId.name.localeCompare(b.eipId.name))
    return { name: namespace, components: filtered }
  })

  return withFilteredComponents.filter(
    ({ components }) => components.length > 0
  )
}

export const EipComponentPanel = () => {
  const [searchTerm, setSearchTerm] = useState("")
  const [expandedNamespace, setExpandedNamespace] = useState("")
  const nodeCount = useNodeCount()

  useEffect(() => setSearchTerm(""), [nodeCount])

  const resetExpandedNamespace = () => {
    if (expandedNamespace !== "" && searchTerm.length > 0) {
      setExpandedNamespace("")
    }
  }

  const collections = namespacesToDisplay(
    Object.entries(EIP_SCHEMA),
    searchTerm
  ).map((ns) => (
    <EipNamespaceCollection
      key={ns.name}
      namespace={ns}
      expandedNamespace={expandedNamespace}
      handleExpand={(isExpanded) => isExpanded && setExpandedNamespace(ns.name)}
      allExpanded={searchTerm.length > 0}
    />
  ))

  return (
    <>
      <div className="search-bar-container">
        <Search
          labelText="Narrow component selections"
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value)
            resetExpandedNamespace()
          }}
        />
      </div>

      <Accordion className="eip-namespace-list" size="lg" isFlush>
        {collections}
      </Accordion>
    </>
  )
}
