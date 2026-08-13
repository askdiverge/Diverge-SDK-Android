# Hosting `docs.askdiverge.ai`

## Current scaffold

[`Docs/site/`](../Docs/site/) is a static HTML/CSS stub. CI builds DocC for **DivergeSDK** and
**DivergeSDKUI**, merges them with that folder into `site-dist/`, and deploys to
**GitHub Pages** (`.github/workflows/docc.yml`).

A `CNAME` file with `docs.askdiverge.ai` is written into `site-dist/` by
[`scripts/build-docs-site.sh`](../scripts/build-docs-site.sh). DNS still needs a
human to point the domain at GitHub Pages.

## Local build

```bash
make docs-docc
# → site-dist/ (guides + documentation/divergesdk + documentation/divergesdkui)
```

## Recommended evolution

| Stage | Approach | Notes |
|-------|----------|-------|
| Now | GitHub Pages from `site-dist/` | DocC + static guides |
| Next | Confirm custom domain + HTTPS on Pages | DNS + org Pages settings |
| Later | VitePress or mdBook for guides | Versioned paths like `/v0.1/` + “latest” redirect |

Keep ATT, getting-started, and integration guides in the public docs site; keep eng-only checklists and templates under `Dev-Docs/`.

DocC operator-overload pages whose **paths** contain `:` (and other NTFS-illegal characters) — including directories like `!=(_:_:)/` — are stripped from `site-dist/` so GitHub Actions artifacts and Pages uploads succeed. Full archives remain under local `docs-out/` after `make docs-docc`.

## Versioning

The site badge is synced from the root `VERSION` file via `./scripts/sync-version.sh`.
When adding versioned docs, prefer tag-based folders rather than overwriting “latest” in place.

## API reference paths

| Product | Path on site |
|---------|----------------|
| `DivergeSDK` | `/documentation/divergesdk/` |
| `DivergeSDKUI` | `/documentation/divergesdkui/` |
