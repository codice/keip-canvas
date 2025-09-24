import {
  Accordion,
  AccordionItem,
  Form,
  Select,
  SelectItem,
  Stack,
  TextInput,
  Toggle,
} from "@carbon/react"
import { ChangeEvent, useMemo } from "react"
import { Attribute, AttributeType } from "../../api/generated/eipComponentDef"
import {
  deleteEipAttribute,
  updateEipAttribute,
} from "../../singletons/store/appActions"
import { useGetEipAttribute } from "../../singletons/store/getterHooks"
import debounce from "../../utils/debounce"
import DescriptionTooltipWrapper from "./DescriptionTooltipWrapper"

const addPaddingClass = "cfg-panel__container__side-padding-add"

interface AttributeInputFactoryProps {
  attr: Attribute
  id: string
}

interface AttributeInputProps<T> {
  id: string
  attr: Attribute
  attrValue: T
}

interface AttributeFormProps {
  attrs: Attribute[]
  id: string
}

const AttributeSelectInput = ({
  attr,
  attrValue,
  id,
}: AttributeInputProps<string>) => {
  const emptySelect = ""
  const options = useMemo(
    () =>
      attr.default
        ? attr.restriction!.values!
        : [emptySelect, ...attr.restriction!.values!],
    [attr.restriction, attr.default]
  )

  const handleSelect = (ev: ChangeEvent<HTMLSelectElement>) => {
    ev.target.value === emptySelect
      ? deleteEipAttribute(id, attr.name)
      : updateEipAttribute(id, attr.name, ev.target.value)
  }

  return (
    <DescriptionTooltipWrapper id={attr.name} description={attr.description}>
      <Select
        id={attr.name}
        labelText={attr.name}
        defaultValue={attrValue ?? attr.default}
        hideLabel={Boolean(attr.description)}
        onChange={handleSelect}
      >
        {options.map((item) => (
          <SelectItem key={item} value={item} text={item} />
        ))}
      </Select>
    </DescriptionTooltipWrapper>
  )
}

const AttributeBoolInput = ({
  attr,
  attrValue,
  id,
}: AttributeInputProps<boolean>) => {
  const handleToggle = (checked: boolean) =>
    updateEipAttribute(id, attr.name, checked)

  return (
    <DescriptionTooltipWrapper id={attr.name} description={attr.description}>
      <div style={{ display: "block" }}>
        <Toggle
          id={attr.name}
          labelText={attr.description ? "" : attr.name}
          labelA=""
          labelB=""
          defaultToggled={attrValue ?? Boolean(attr.default)}
          hideLabel={Boolean(attr.description)}
          onToggle={handleToggle}
        />
      </div>
    </DescriptionTooltipWrapper>
  )
}

const getDefaultStr = (attr: Attribute) =>
  attr.default ? String(attr.default) : ""

const AttributeTextInput = ({
  attr,
  attrValue,
  id,
}: AttributeInputProps<string>) => {
  const handleTextUpdates = useMemo(
    () =>
      debounce((ev: ChangeEvent<HTMLInputElement>) => {
        ev.target.value === ""
          ? deleteEipAttribute(id, attr.name)
          : updateEipAttribute(id, attr.name, ev.target.value)
      }, 300),
    [id, attr.name]
  )

  return (
    <DescriptionTooltipWrapper id={attr.name} description={attr.description}>
      <TextInput
        id={attr.name}
        labelText={attr.name}
        defaultValue={attrValue ?? getDefaultStr(attr)}
        hideLabel={Boolean(attr.description)}
        onChange={handleTextUpdates}
      />
    </DescriptionTooltipWrapper>
  )
}

const AttributeInput = (props: AttributeInputFactoryProps) => {
  const attrValue = useGetEipAttribute(props.id, props.attr.name)

  switch (props.attr.type) {
    // TODO: Handle number types with more specific input components (e.g. NumberInput, Slider).
    case "number":
    case "string":
      if (props.attr.restriction?.values) {
        return (
          <AttributeSelectInput {...props} attrValue={attrValue as string} />
        )
      }
      return <AttributeTextInput {...props} attrValue={attrValue as string} />

    case "boolean":
      return <AttributeBoolInput {...props} attrValue={toBoolean(attrValue)} />
  }
}

const toBoolean = (value: AttributeType) => {
  if (typeof value === "string") {
    return value === "true"
  }
  return Boolean(value)
}

export const AttributeConfigForm = (props: AttributeFormProps) => {
  const required = props.attrs
    .filter((attr) => attr.required)
    .sort((a, b) => a.name.localeCompare(b.name))
  const optional = props.attrs
    .filter((attr) => !attr.required)
    .sort((a, b) => a.name.localeCompare(b.name))

  return (
    <Form onSubmit={(e) => e.preventDefault()}>
      <Accordion isFlush size="lg">
        {required.length ? (
          <AccordionItem title="Required" open>
            <Stack gap={6} className={addPaddingClass}>
              {required.map((attr) => (
                <AttributeInput {...props} key={attr.name} attr={attr} />
              ))}
            </Stack>
          </AccordionItem>
        ) : null}
        {optional.length ? (
          <AccordionItem title="Optional">
            <Stack gap={6} className={addPaddingClass}>
              {optional.map((attr) => (
                <AttributeInput {...props} key={attr.name} attr={attr} />
              ))}
            </Stack>
          </AccordionItem>
        ) : null}
      </Accordion>
    </Form>
  )
}
