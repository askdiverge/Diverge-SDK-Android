# Diverge SDK for Android

Open-source ecommerce SDK for **Android**.

> iOS SDK: [Diverge-SDK-iOS](https://github.com/mohamedaldahoul/Diverge-SDK-iOS) · Hub: [Diverge-SDK](https://github.com/DialogIntelligens/Diverge-SDK)

## Requirements

| | |
|--|--|
| minSdk | **24** |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.0+ |
| JDK | 17 |

minSdk 24 is intentional. Do **not** add desugar / joda-time (or similar) backports solely for older APIs.

`DivergeStatusView` is built **programmatically** — no layout XML ships in the library. The sample app may still use XML.

## Installation

```kotlin
implementation(project(":diverge-sdk"))
// after Central: implementation("ai.askdiverge:diverge-sdk:0.1.0")
```

Prefer version pins / GitHub Releases — do not track `main`.

## License

[Apache-2.0](LICENSE.md) — Copyright © 2026 Diverge
