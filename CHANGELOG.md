# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-08-13

### Added

- Public API: `Configuration`, `Environment`, `DivergeClient`, `Diverge.configure` / `shared`
- Programmatic `DivergeStatusView` (no layout XML in the library)
- Paparazzi goldens; narrow ProGuard consumer rules; R8 keep verification via sample
- Maven Central publish-ready wiring (signing + Central Portal staging)
- SemVer GitHub Releases via `v*` tags

### Changed

- Relicensed to Apache License, Version 2.0
- Documented minSdk 24 policy (no desugar/joda-style backports)
- Extracted from the former monorepo into a dedicated Android repository
