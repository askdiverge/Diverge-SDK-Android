# Changelog — Diverge SDK Android

## [Unreleased]

## [0.1.1] - 2026-08-17

### Added

- Committed Paparazzi golden PNGs for `DivergeStatusView`
- `make android-paparazzi-record` target
- Operator runbooks under `Dev-Docs/ops/`
- Android-only Dev-Docs cleanup (removed iOS/hub leftovers)
- Integration guide `Dev-Docs/integration/v0.1.0.md` + release process docs

### Changed

- Removed unused `androidx.core:core-ktx` from the library module
- `DivergeStatusView` API base URL content description uses a string resource
- README drops obsolete hub link; points at the iOS sibling repo for privacy docs

## [0.1.0] - 2026-08-13

### Added

- Public API: `Configuration`, `Environment`, `DivergeClient`, `Diverge.configure` / `shared`
- Programmatic `DivergeStatusView` (no SDK layout XML)
- Paparazzi; ProGuard consumer rules; Maven Central publish wiring
- SemVer GitHub Releases

### Changed

- Apache License 2.0
- Documented minSdk 24 (no desugar/joda-style backports)
- Split out of the former monorepo into this dedicated Android repository
