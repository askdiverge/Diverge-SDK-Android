# Operator runbook: canary / beta GitHub Release (Android)

**Status:** release workflow supports prerelease tags; smoke-publish is an operator action.

## Tag conventions

| Channel | Tag pattern | GitHub Release |
|---------|-------------|----------------|
| Stable | `vMAJOR.MINOR.PATCH` | `prerelease: false` |
| Beta | `vMAJOR.MINOR.PATCH-beta.N` | `prerelease: true` |
| Canary | `vMAJOR.MINOR.PATCH-canary.N` | `prerelease: true` |

Enforced by [`.github/workflows/release.yml`](../../.github/workflows/release.yml).

## Preconditions

1. Root `VERSION` matches the base SemVer (e.g. `v0.1.0-canary.1` ⇒ `VERSION` = `0.1.0`).
2. `CHANGELOG.md` has a matching `## [...]` entry.
3. `./scripts/check-version.sh` passes.
4. `Android` workflow is green (includes Paparazzi goldens via `:diverge-sdk:test`).

## Smoke steps (operator)

```bash
git tag v0.1.0-canary.1
git push origin v0.1.0-canary.1
```

Confirm the Release workflow creates a **Pre-release**. Maven Central publish remains a separate manual workflow — see [`../releases/MAVEN_CENTRAL.md`](../releases/MAVEN_CENTRAL.md).
