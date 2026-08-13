# Diverge SDK for Android

Open-source ecommerce SDK for **Android** (Gradle library module). Maven Central publish wiring is included; first Central deploy still needs Portal + GPG secrets.

> Platform SDKs live in separate repositories. iOS: [Diverge-SDK-iOS](https://github.com/mohamedaldahoul/Diverge-SDK-iOS). Overview hub: [Diverge-SDK](https://github.com/DialogIntelligens/Diverge-SDK).

## Requirements

| | |
|--|--|
| minSdk | **24** |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.0+ |
| JDK | 17 |

minSdk 24 is intentional. Do **not** add desugar / joda-time (or similar) backports solely to paper over older API gaps.

The published SDK UI (`DivergeStatusView`) is built programmatically — no layout XML ships in the library. The sample app may still use XML layouts.

## Installation

Until Maven Central publish:

```kotlin
implementation(project(":diverge-sdk"))
```

After Central:

```kotlin
implementation("ai.askdiverge:diverge-sdk:0.1.0")
```

```kotlin
Diverge.configure(
    Configuration(apiKey = "sk_sandbox_demo", environment = Environment.SANDBOX)
)
val client = Diverge.shared
```

Environment wire names: `sandbox` / `production` (`Environment.wireName`).

## Versioning

Root [`VERSION`](VERSION) file. GitHub Releases are created from SemVer tags (`v0.1.0`). Prefer version pins — do not track `main`.

```bash
./scripts/sync-version.sh
./scripts/check-version.sh
```

## Sample

[`sample/`](sample) — release builds enable R8 minify; `:sample:verifyR8PublicApiKeeps` checks consumer keep rules.

## License

[Apache-2.0](LICENSE.md) — Copyright © 2026 Diverge
