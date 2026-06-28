import type { Plugin } from "@opencode-ai/plugin"

const COMMIT_CONVENTION = /^(feat|fix|refactor|test|docs|chore|ci|perf|security)\((identity|wallet|payment|fraud|notification|audit|reporting|gateway|infra|frontend)\):\s.+/

const SECRET_PATTERNS = [
  /(?:password|passwd|pwd)\s*[:=]\s*["'][^"']+["']/gi,
  /(?:api[_-]?key|apikey)\s*[:=]\s*["'][^"']+["']/gi,
  /(?:AKIA|ASIA)[A-Z0-9]{16}/g,
  /-----BEGIN\s+(RSA\s+)?PRIVATE\s+KEY-----/g,
]

export default (async ({ project, $ }) => {
  return {
    "tool.execute.before": async (input, output) => {
      if (output.tool !== "bash") return

      const command = String(output.args?.command ?? "")

      if (!command.startsWith("git commit")) return

      const messageMatch = command.match(/-m\s+["'](.+?)["']/)
      if (messageMatch) {
        const message = messageMatch[1]
        if (!COMMIT_CONVENTION.test(message)) {
          output.args._commitWarning =
            `[pre-commit-guard] Commit message does not follow convention.\n` +
            `Expected: <type>(<scope>): <description>\n` +
            `Types: feat, fix, refactor, test, docs, chore, ci, perf, security\n` +
            `Scopes: identity, wallet, payment, fraud, notification, audit, reporting, gateway, infra, frontend\n` +
            `Got: "${message}"`
        }
      }

      try {
        const diffResult = await $`git diff --cached --diff-filter=ACM`
        const diff = String(diffResult.stdout ?? "")

        for (const pattern of SECRET_PATTERNS) {
          if (pattern.test(diff)) {
            output.args._secretWarning =
              `[pre-commit-guard] Potential secret detected in staged changes. ` +
              `Review and remove before committing. Never commit secrets to the repository.`
            break
          }
          pattern.lastIndex = 0
        }
      } catch {
        // git diff may fail if no staged changes
      }
    },
  }
}) satisfies Plugin
