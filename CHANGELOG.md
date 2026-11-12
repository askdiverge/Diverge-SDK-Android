# Changelog — Diverge SDK Android

## [Unreleased]

### Added

- After a livechat session closes with feedback pending, a CSAT prompt asks for a 1–5 rating
  (`POST /livechat/feedback`); scores 1–3 can include an optional comment, and Skip dismisses
  without posting. A session found already closed when the chat starts is not asked; one that
  ends while the chat is hidden is asked when it is shown again
- While queued for a live agent, a configured `livechat_waiting` form from `/config` can be filled
  and saved (`GET /session`, `PATCH /forms/{id}/values`) so the agent sees the visitor's details.
  Visitor photo attach is not included yet
- Livechat visitor typing and agent typing indicator: while an agent is connected, draft changes
  POST `is_typing` on `/livechat/typing`; sync `is_agent_typing` drives the composer typing row
- Agent file and image message parts render in the conversation; temporary download links are
  re-signed from the livechat log on tap. Visitor photo attach in livechat is not included yet
- Livechat handover and conversation UI: header control to start or end a session, the
  `request_human_agent` card, session notes (queued / agent joined / ended), composer placeholders
  by status, and a leave/end confirmation
- While an agent is connected (`active`), visitor sends go to the agent over
  `POST /livechat/messages` instead of the AI stream; while queued (`waiting`) they still go to
  the assistant. While a session is open and the chat is on screen, the SDK polls
  `GET /livechat/sync` so agent replies appear in the conversation
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
