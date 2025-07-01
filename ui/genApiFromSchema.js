import $RefParser from "@apidevtools/json-schema-ref-parser"
import { compile } from "json-schema-to-typescript"
import fs from "node:fs"
import { writeFile } from "node:fs/promises"
import path from "node:path"

// TODO: Look into avoiding duplication of generated types when multiple top-level schemas
// reference the same common schemas.

// TODO: Use git tags instead of commit hashes
// To use an updated source schema, change this to point to the desired version.
const COMMIT_HASH = "4fe59e1438fad31b79c87568686cf867fd14a41e"

const SCHEMAS = ["eipComponentDef.schema.json", "eipFlow.schema.json"]

const SCHEMAS_BASE_URL = "https://raw.githubusercontent.com"

const GENERATED_SOURCE_DIR = path.resolve("src", "api", "generated")

const buildUrl = (schemaFileName) =>
  new URL(
    `codice/keip-canvas/${COMMIT_HASH}/schemas/model/json/${schemaFileName}`,
    SCHEMAS_BASE_URL
  ).toString()

const generateSources = async (schemaFileName) => {
  const bundledSchema = await $RefParser.bundle(buildUrl(schemaFileName))

  const generatedCode = await compile(bundledSchema, bundledSchema.title, {
    additionalProperties: false,
  })

  const outputFileName = `${schemaFileName.split(".")[0]}.ts`
  const outputPath = path.join(GENERATED_SOURCE_DIR, outputFileName)
  
  console.log(`generating ${outputPath}`)
  
  await writeFile(outputPath, generatedCode)
}

fs.mkdirSync(GENERATED_SOURCE_DIR, { recursive: true })
await Promise.all(SCHEMAS.map(generateSources))
