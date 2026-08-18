# Operator runbook: sandbox backend requirements

**Status:** SDK-only placeholder hosts exist. Real keys, stats isolation, and webhooks are **backend/ops** — not in this Android repo.

## Requirements

| Item | Requirement | Owner |
|------|-------------|-------|
| Sandbox API keys | Keys that only work against sandbox | Backend / platform |
| Sandbox endpoints | Must not affect production stats | Backend |
| Dashboard stats reset | Independent of prod data | Backend / dashboard |
| Webhooks | Separate prod vs dev/sandbox endpoints + secrets | Backend |

## SDK contract today

- `DivergeEnvironment.SANDBOX` → `https://sandbox.api.askdiverge.ai`
- `DivergeEnvironment.PRODUCTION` → `https://api.askdiverge.ai`
- `configure` does not perform network I/O in v0.1.0

Sample apps ship placeholder key `sk_sandbox_demo` only.
