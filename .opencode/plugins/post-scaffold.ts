import type { Plugin } from "@opencode-ai/plugin"

export default (async ({ project }) => {
  return {
    "tool.execute.after": async (input, output) => {
      if (input.tool !== "write" && input.tool !== "edit") return

      const filePath = String(input.args?.filePath ?? input.args?.path ?? "")

      const isServiceScaffold =
        filePath.includes("pom.xml") &&
        filePath.includes("aegis-") &&
        filePath.includes("-service")

      if (!isServiceScaffold) return

      const serviceName = filePath.match(/aegis-(\w+)-service/)?.[1] ?? "unknown"

      console.log(
        `[post-scaffold] Service "${serviceName}" detected. Consider these follow-up steps:\n` +
        `  1. Create an ADR: Use the "create-adr" skill to document the service's architectural decisions\n` +
        `  2. Design the API: Use the "api-design" skill to create REST contracts\n` +
        `  3. Design events: Use the "event-design" skill to define Kafka event schemas\n` +
        `  4. Generate tests: Use the @test-engineer agent to create unit and integration tests\n` +
        `  5. Add infrastructure: Use the @infra-engineer agent for Dockerfile, Helm chart, and CI/CD\n` +
        `  6. Security review: Use the @security-reviewer agent to validate security configuration\n` +
        `  7. Architecture review: Use the @architect agent to validate DDD boundaries`
      )
    },
  }
}) satisfies Plugin
