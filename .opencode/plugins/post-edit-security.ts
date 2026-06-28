import type { Plugin } from "@opencode-ai/plugin"

const SECRET_PATTERNS = [
  { pattern: /(?:password|passwd|pwd)\s*[:=]\s*["'][^"']+["']/gi, name: "hardcoded password" },
  { pattern: /(?:api[_-]?key|apikey)\s*[:=]\s*["'][^"']+["']/gi, name: "hardcoded API key" },
  { pattern: /(?:secret|token|auth)\s*[:=]\s*["'][^"']+["']/gi, name: "hardcoded secret/token" },
  { pattern: /(?:AKIA|ASIA)[A-Z0-9]{16}/g, name: "AWS access key" },
  { pattern: /-----BEGIN\s+(RSA\s+)?PRIVATE\s+KEY-----/g, name: "private key" },
  { pattern: /Bearer\s+[A-Za-z0-9\-._~+/]+=*/g, name: "bearer token" },
]

const INSECURE_PATTERNS = [
  { pattern: /http:\/\/(?!localhost|127\.0\.0\.1|0\.0\.0\.0)/g, name: "non-localhost HTTP URL" },
  { pattern: /\.log\(\s*["'].*(?:password|token|secret|key|ssn|card)/gi, name: "sensitive data in logs" },
  { pattern: /System\.out\.print/g, name: "System.out (use SLF4J logger)" },
  { pattern: /e\.printStackTrace\(\)/g, name: "printStackTrace (use proper logging)" },
]

export default (async ({ project }) => {
  return {
    "tool.execute.after": async (input, output) => {
      if (input.tool !== "edit" && input.tool !== "write") return

      const filePath = String(input.args?.filePath ?? input.args?.path ?? "")
      if (!filePath.match(/\.(java|ts|js|yml|yaml|properties|xml|json)$/)) return

      const content = String(input.args?.content ?? input.args?.newString ?? "")
      const warnings: string[] = []

      for (const { pattern, name } of SECRET_PATTERNS) {
        if (pattern.test(content)) {
          warnings.push(`Potential ${name} detected`)
        }
        pattern.lastIndex = 0
      }

      for (const { pattern, name } of INSECURE_PATTERNS) {
        if (pattern.test(content)) {
          warnings.push(`Insecure pattern: ${name}`)
        }
        pattern.lastIndex = 0
      }

      if (warnings.length > 0) {
        const message =
          `[post-edit-security] Security warnings in ${filePath}:\n` +
          warnings.map((w) => `  - ${w}`).join("\n") +
          "\nReview and fix before committing."
        console.warn(message)
      }
    },
  }
}) satisfies Plugin
