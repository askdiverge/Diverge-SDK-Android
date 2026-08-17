# Contributing (Android)

This repository is **Android-only**. iOS lives in [Diverge-SDK-iOS](https://github.com/askdiverge/Diverge-SDK-iOS).

## Setup

1. JDK 17 + Android SDK (`ANDROID_HOME`)
2. From repo root:

```bash
./gradlew :diverge-sdk:assemble :diverge-sdk:test :diverge-sdk:lint
make android-test
```

Re-record Paparazzi goldens after intentional StatusView UI changes:

```bash
make android-paparazzi-record
```

## Releases

1. Bump `VERSION`, run `./scripts/sync-version.sh` if needed, update `CHANGELOG.md`.
2. For breaking changes, add `Dev-Docs/integration/vX.Y.Z.md` from [`Dev-Docs/integration/TEMPLATE.md`](Dev-Docs/integration/TEMPLATE.md).
3. Tag SemVer: `vMAJOR.MINOR.PATCH` or `vX.Y.Z-beta.N` / `vX.Y.Z-canary.N`.
4. Push the tag; `.github/workflows/release.yml` creates the GitHub Release.
5. Optionally publish to Maven Central via `publish-android.yml` — see [`Dev-Docs/releases/MAVEN_CENTRAL.md`](Dev-Docs/releases/MAVEN_CENTRAL.md).

See [`Dev-Docs/ops/canary-release.md`](Dev-Docs/ops/canary-release.md).

## License

By contributing, you agree contributions are licensed under Apache License 2.0.
