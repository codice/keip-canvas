import { Stack } from "@carbon/react"
import { supportError } from "@carbon/themes"
import hljs from "highlight.js/lib/core"
import json from "highlight.js/lib/languages/json"
import xml from "highlight.js/lib/languages/xml"
import Editor from "react-simple-code-editor"

hljs.registerLanguage("json", json)
hljs.registerLanguage("xml", xml)

interface ModalCodeEditorProps {
  content: string
  setContent: (content: string) => void
  language: "json" | "xml"
  helperText?: string
  invalid?: boolean
  invalidText?: string
}

export const ModalCodeEditor = ({
  content,
  setContent,
  language,
  helperText,
  invalid,
  invalidText,
}: ModalCodeEditorProps) => {
  const errorOutline = invalid ? { outline: `2px solid ${supportError}` } : {}

  return (
    <Stack gap={3}>
      <div className="modal__code-editor" style={errorOutline}>
        <Editor
          value={content}
          onValueChange={(code) => setContent(code)}
          highlight={(code) => hljs.highlight(code, { language }).value}
          padding={16}
          textareaClassName="modal__code-editor-textarea"
        />
      </div>
      {invalid && <p className="modal__error-message">{invalidText}</p>}
      {!invalid && helperText && (
        <p className="modal__helper-text">{helperText}</p>
      )}
    </Stack>
  )
}
