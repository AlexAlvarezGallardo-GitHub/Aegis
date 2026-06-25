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

function isDomainLayer(filePath: string): boolean {
  return DOMAIN_LAYER_PATTERNS.some((pattern) => filePath.includes(pattern))
}

function isControllerLayer(filePath: string): boolean {
  return filePath.includes("web/controller/")
}

export default (async ({ project }) => {
  return {
    "tool.execute.before": async (input, output) => {
      if (output.tool !== "edit" && output.tool !== "write") return

      const filePath = String(output.args?.filePath ?? output.args?.path ?? "")
      const content = String(output.args?.content ?? output.args?.newString ?? "")

      if (!filePath.match(/\.(java|kt)$/)) return

      if (isDomainLayer(filePath)) {
        const violations = FORBIDDEN_DOMAIN_IMPORTS.filter((imp) =>
          content.includes(imp)
        )
        if (violations.length > 0) {
          output.args._guardWarning =
            `[pre-edit-guard] Domain layer violation detected in ${filePath}. ` +
            `Forbidden imports: ${violations.join(", ")}. ` +
            `Domain model must not depend on infrastructure, web, or persistence layers.`
        }
      }

      if (isControllerLayer(filePath)) {
        const violations = FORBIDDEN_CONTROLLER_IMPORTS.filter((imp) =>
          content.includes(imp)
        )
        if (violations.length > 0) {
          output.args._guardWarning =
            `[pre-edit-guard] Controller layer violation in ${filePath}. ` +
            `Direct repository access detected: ${violations.join(", ")}. ` +
            `Controllers must use application services, not repositories.`
        }
      }
    },
  }
}) satisfies Plugin
