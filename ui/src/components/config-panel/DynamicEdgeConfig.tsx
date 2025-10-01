import {
  Heading,
  Section,
  SideNavDivider,
  Stack,
  TextInput,
  Toggle,
} from "@carbon/react"
import { ChangeEvent, useMemo } from "react"
import { ChannelMapping, DynamicEdge, RouterKeyDef } from "../../api/flow"
import { lookupContentBasedRouterKeys } from "../../singletons/eipDefinitions"
import {
  updateContentRouterKey,
  updateDynamicEdgeMapping,
} from "../../singletons/store/appActions"
import {
  useGetContentRouterKey,
  useGetRouterDefaultEdgeMapping,
} from "../../singletons/store/getterHooks"
import { getEipId, getNodesView } from "../../singletons/store/storeViews"
import debounce from "../../utils/debounce"
import DescriptionTooltipWrapper from "./DescriptionTooltipWrapper"

interface SectionHeadingProps {
  title: string
  helperText?: string
}

interface DefaultMappingToggleProps {
  edgeId: string
  toggled?: boolean
  disabled?: boolean
}

interface RouterKeyProps {
  routerNodeId: string
  routerKeyDef: RouterKeyDef
}

interface EdgeMatcherProps {
  edgeId: string
  mapping: ChannelMapping
}

interface EdgeConfigProps {
  edge: DynamicEdge
}

const addPaddingClass = "cfg-panel__container__side-padding-add"

const SectionHeading = ({ title, helperText }: SectionHeadingProps) => (
  <Stack gap={2}>
    <Heading>{title}</Heading>
    <p className="sec-header__helper-text">{helperText}</p>
  </Stack>
)

const DefaultMappingToggle = ({
  edgeId,
  toggled,
  disabled,
}: DefaultMappingToggleProps) => {
  const id = "isDefaultMapping"
  const description =
    "A message is routed through this edge when routing resolution fails to match any of the defined routing matchers. At most one outgoing edge can marked as the default output."

  return (
    <Section>
      <DescriptionTooltipWrapper id={id} description={description}>
        <div style={{ display: "block" }}>
          <Toggle
            id={id}
            labelText=""
            labelA=""
            labelB=""
            defaultToggled={toggled}
            disabled={disabled}
            hideLabel
            onToggle={(checked) =>
              updateDynamicEdgeMapping(edgeId, { isDefaultMapping: checked })
            }
          />
        </div>
      </DescriptionTooltipWrapper>
    </Section>
  )
}

// TODO: Consider moving router key configuration to the Node side panel
// TODO: See if we can re-use (or generalize) the AttributeTextInput component here.
const RouterKeyConfig = ({ routerNodeId, routerKeyDef }: RouterKeyProps) => {
  const currRouterKey = useGetContentRouterKey(routerNodeId)

  const handleUpdates = useMemo(
    () =>
      debounce((attrName: string, value: string) => {
        updateContentRouterKey(
          routerNodeId,
          {
            namespace: routerKeyDef.eipId.namespace,
            name: routerKeyDef.eipId.name,
          },
          attrName,
          value
        )
      }, 300),
    [routerNodeId, routerKeyDef.eipId.name, routerKeyDef.eipId.namespace]
  )

  const required = routerKeyDef.attributesDef
    .filter((attr) => attr.required)
    .sort((a, b) => a.name.localeCompare(b.name))

  const optional = routerKeyDef.attributesDef
    .filter((attr) => !attr.required)
    .sort((a, b) => a.name.localeCompare(b.name))

  const inputs = [...required, ...optional].map((attr) => (
    <DescriptionTooltipWrapper
      key={attr.name}
      id={attr.name}
      description={attr.description}
    >
      <TextInput
        id={attr.name}
        labelText={attr.required ? `${attr.name} (required)` : attr.name}
        defaultValue={String(currRouterKey?.attributes?.[attr.name] ?? "")}
        hideLabel={Boolean(attr.description)}
        onChange={(ev) => handleUpdates(attr.name, ev.target.value)}
      />
    </DescriptionTooltipWrapper>
  ))

  return (
    <Section>
      <Stack gap={6}>
        <SectionHeading
          title="Key"
          helperText={
            routerKeyDef.attributesDef.length !== 1
              ? routerKeyDef.eipId.name
              : "Targeted by the matcher"
          }
        />
        {inputs}
      </Stack>
    </Section>
  )
}

// TODO: Allow mapping multiple values to the same channel
const EdgeMatcher = ({ edgeId, mapping }: EdgeMatcherProps) => {
  const { mapperId, matcher, matcherValue } = mapping

  const handleUpdates = useMemo(
    () =>
      debounce((ev: ChangeEvent<HTMLInputElement>) => {
        updateDynamicEdgeMapping(edgeId, { matcherValue: ev.target.value })
      }, 300),
    [edgeId]
  )

  return (
    <Section>
      <Stack gap={6}>
        <SectionHeading title="Matcher" helperText={mapperId.name} />
        <DescriptionTooltipWrapper
          key={matcher.name}
          id={matcher.name}
          description={matcher.description}
        >
          <TextInput
            id={matcher.name}
            labelText={matcher.name}
            defaultValue={matcherValue ?? ""}
            hideLabel={Boolean(matcher.description)}
            onChange={handleUpdates}
          />
        </DescriptionTooltipWrapper>
      </Stack>
    </Section>
  )
}

const DynamicEdgeConfig = ({ edge }: EdgeConfigProps) => {
  const currDefaultEdge = useGetRouterDefaultEdgeMapping(edge.source)
  const sourceNode = getNodesView().find((node) => node.id === edge.source)
  const sourceEipId = sourceNode && getEipId(sourceNode.id)
  const routerKeyDef = sourceEipId && lookupContentBasedRouterKeys(sourceEipId)

  const isTheDefaultEdge = currDefaultEdge?.id === edge.id

  // TODO: Fix add-padding class. Add padding to sides only.
  // Vertical padding should be handled by the Stack container.
  return (
    <Section level={3} className="edge-config-container">
      <Stack gap={4}>
        <Stack gap={6} className={addPaddingClass}>
          <Heading>Edge</Heading>
          <h5>{`Source: ${edge.source}`}</h5>
          <h5>{`Target: ${edge.target}`}</h5>
        </Stack>
        <SideNavDivider />
        <Stack gap={7} className={addPaddingClass}>
          <SectionHeading
            title="Routing"
            helperText="Content-based routing configuration"
          />
          <DefaultMappingToggle
            edgeId={edge.id}
            toggled={isTheDefaultEdge}
            disabled={!isTheDefaultEdge && Boolean(currDefaultEdge)}
          />
          {!isTheDefaultEdge && (
            <>
              {routerKeyDef && (
                <RouterKeyConfig
                  routerNodeId={edge.source}
                  routerKeyDef={routerKeyDef}
                />
              )}
              {edge.data?.mapping && (
                <EdgeMatcher edgeId={edge.id} mapping={edge.data.mapping} />
              )}
            </>
          )}
        </Stack>
      </Stack>
    </Section>
  )
}

export default DynamicEdgeConfig
