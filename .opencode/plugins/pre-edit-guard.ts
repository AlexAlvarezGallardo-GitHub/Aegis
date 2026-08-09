import type { Plugin } from "@opencode-ai/plugin"

const DOMAIN_LAYER_PATTERNS = [
  "domain/model/",
  "domain/event/",
  "domain/exception/",
  "domain/port/",
]

const FORBIDDEN_DOMAIN_IMPORTS = [
  "infrastructure.",
  "persistence.",
  "messaging.",
  "controller.",
  "springframework.web",
  "springframework.data.jpa",
  "jakarta.persistence",
  "org.apache.kafka",
]

const FORBIDDEN_CONTROLLER_IMPORTS = [
  "repository.",
  "Repository",
]

// Normalize Windows separators so the layer checks work on every OS.
function normalizePath(filePath: string): string {
  return filePath.replace(/\\/g, "/")
}

function isDomainLayer(filePath: string): boolean {
  return DOMAIN_LAYER_PATTERNS.some((pattern) => filePath.includes(pattern))
}

function isControllerLayer(filePath: string): boolean {
  return filePath.includes("web/controller/")
}

export default (async ({ project }) => {
  return {
    "tool.execute.before": async (input, output) => {
      // The tool name lives on `input`, not `output` — reading `output.tool`
      // made this guard a silent no-op.
      if (input.tool !== "edit" && input.tool !== "write") return

      const filePath = normalizePath(
        String(output.args?.filePath ?? output.args?.path ?? "")
      )
      const content = String(output.args?.content ?? output.args?.newString ?? "")

      if (!filePath.match(/\.(java|kt)$/)) return

      if (isDomainLayer(filePath)) {
        const violations = FORBIDDEN_DOMAIN_IMPORTS.filter((imp) =>
          content.includes(imp)
        )
        if (violations.length > 0) {
          console.warn(
            `[pre-edit-guard] Domain layer violation detected in ${filePath}. ` +
              `Forbidden imports: ${violations.join(", ")}. ` +
              `Domain model must not depend on infrastructure, web, or persistence layers.`
          )
        }
      }

      if (isControllerLayer(filePath)) {
        const violations = FORBIDDEN_CONTROLLER_IMPORTS.filter((imp) =>
          content.includes(imp)
        )
        if (violations.length > 0) {
          console.warn(
            `[pre-edit-guard] Controller layer violation in ${filePath}. ` +
              `Direct repository access detected: ${violations.join(", ")}. ` +
              `Controllers must use application services, not repositories.`
          )
        }
      }
    },
  }
}) satisfies Plugin
