# Changelog — Diverge SDK Android

## [Unreleased]

### Added

- `X-Diverge-SDK-Platform: android`, `X-Diverge-SDK-Version` and `X-Diverge-Client-Profile` on
  every authenticated API call (`/config`, `/messages`). Platform and version identify the
  calling SDK release; the profile header lists every feature the build requests
  (comma-separated: `product-recommendation`, `customer-service`), and the backend enables the
  tools for the ones it grants, so one chat can combine features. Image downloads carry only the
  request itself. Hosts need no change
- In-chat contact, support-ticket and custom forms (`show_contact_form` /
  `show_support_ticket` / `show_form`), gated by the `customer-service` client profile
- `environment` parameter on `ChatbotBottomSheet`, taking `DivergeEnvironment`
  (`PRODUCTION` / `DEVELOPMENT`), so a host can integrate against the development API.
  Defaults to `PRODUCTION`, so an existing host keeps talking to the live API without changes.
  The sample uses `DEVELOPMENT` in debug builds
- `attachments` parameter on `ChatbotBottomSheet`, taking `ChatbotAttachments`
  (`PHOTO_LIBRARY` / `DISABLED`). Defaults to `PHOTO_LIBRARY`, so custom-form file fields and
  support-ticket photos use the Android Photo Picker (no storage permission). Pass `DISABLED`
  when the host must not access photos; required file fields then make the form unavailable

### Changed

- The sample is a minimal Compose host — paste a session token, open the chat in a bottom sheet —
  replacing the removed `DivergeStatusView` screen
- Accessibility: AA-safe StatusView colors; TalkBack heading/live region; sample error TextView; refreshed Paparazzi golden; clearer TalkBack/WCAG checklists (device gesture sign-off still operator-owned)

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
