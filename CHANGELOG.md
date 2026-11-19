# Changelog — Diverge SDK Android

## [Unreleased]

### Fixed

- The header subtitle shows the inline formatting the dashboard allows, `**bold**` and
  `[label](url)` links, instead of the raw markup, as the web widget does. A link is tappable when
  its URL is an absolute http(s) URL with a host, and opens that URL exactly as written; any other
  link (`mailto:`, `tel:`, a relative path, a URL with spaces or quotes) shows its label as plain
  text. Hosts need no change

## [1.0.0] - 2026-11-14

First release on Maven Central. Replaces the 0.1.0 surface; see
[`Dev-Docs/integration/v1.0.0.md`](Dev-Docs/integration/v1.0.0.md).

### Added

- Public API: `ChatbotBottomSheet`, the chat as a Compose modal bottom sheet; `ChatbotCallbacks`,
  the host's `suspend` token and delete-data hooks returning `Result`; `ChatbotException`;
  `ChatbotLogger`; `DivergeEnvironment`. Consumer R8 rules ship in the AAR
- `X-Diverge-SDK-Platform: android`, `X-Diverge-SDK-Version` and `X-Diverge-Client-Profile` on
  every authenticated API call (`/config`, `/messages`). Platform and version identify the
  calling SDK release; the profile header lists every feature the build requests
  (comma-separated, `product-recommendation` today), and the backend enables the tools for the
  ones it grants, so one chat can combine features. Image downloads carry only the request itself.
  Hosts need no change
- `environment` parameter on `ChatbotBottomSheet`, taking `DivergeEnvironment`
  (`PRODUCTION` / `DEVELOPMENT`), so a host can integrate against the development API.
  Defaults to `PRODUCTION`, so an existing host keeps talking to the live API without changes.
  The sample uses `DEVELOPMENT` in debug builds

### Changed

- The sample is a minimal Compose host — paste a session token, open the chat in a bottom sheet —
  replacing the removed `DivergeStatusView` screen
- While a reply streams, the assistant bubble draws a rotating gradient border in the `/config`
  `thinking_border_gradient` colours, and none when that list is empty. A finished assistant
  bubble has no border unless `/config` sets one

### Removed

- The 0.1.0 API in `ai.askdiverge.sdk`: `Diverge.configure` / `Diverge.shared`, `Configuration`,
  `DivergeClient`, `DivergeException`, `Environment` (`SANDBOX` / `PRODUCTION`) and
  `DivergeStatusView`. Use `ChatbotBottomSheet` with `ChatbotCallbacks`

### Fixed

- Product card images load again: Coil 3 needs `coil-network-okhttp` for remote URLs; without it
  `AsyncImage` left the `surfaceVariant` placeholder blank

## [0.1.0] - 2026-08-13

### Added

- Public API: `Configuration`, `Environment`, `DivergeClient`, `Diverge.configure` / `shared`
- Programmatic `DivergeStatusView` (no SDK layout XML)
- Paparazzi goldens for StatusView; ProGuard consumer rules; Maven Central publish wiring
- SemVer GitHub Releases
- `make android-paparazzi-record` target
- Operator runbooks under `Dev-Docs/ops/`
- Integration guides under `Dev-Docs/integration/`

### Changed

- Apache License 2.0
- Documented minSdk 24 (no desugar/joda-style backports)
- Split out of the former monorepo into this dedicated Android repository
- Android-only Dev-Docs (removed iOS/hub leftovers)
- Removed unused `androidx.core:core-ktx` from the library module
- `DivergeStatusView` API base URL content description uses a string resource
