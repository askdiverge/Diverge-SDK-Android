# Integration guides (Android)

| File | Purpose |
|------|---------|
| [`TEMPLATE.md`](TEMPLATE.md) | Skeleton for a new release guide |
| [`v0.1.0.md`](v0.1.0.md) | Baseline public surface (stay on 0.1.0 until API is stable) |

## Process (every tagged release)

1. Bump `VERSION`, update `CHANGELOG.md`.
2. If breaking changes, copy `TEMPLATE.md` → `vX.Y.Z.md`.
3. Fill GitHub Release from [`../releases/RELEASE_NOTES_TEMPLATE.md`](../releases/RELEASE_NOTES_TEMPLATE.md).
4. Tag and push — see [`../ops/canary-release.md`](../ops/canary-release.md).
