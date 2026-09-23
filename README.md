# Diverge SDK for Android

Open-source ecommerce SDK for **Android**.

> iOS SDK: [Diverge-SDK-iOS](https://github.com/askdiverge/Diverge-SDK-iOS)

## Requirements

| | |
|--|--|
| minSdk | **24** |
| compileSdk / targetSdk | 35 |
| Kotlin | 2.0+ |
| JDK | 17 |

## Installation

```kotlin
dependencies {
    implementation("ai.askdiverge:diverge-sdk:0.1.0")
}
```

Coordinates: group `ai.askdiverge`, artifact `diverge-sdk`, from [Maven Central](https://central.sonatype.com/).

For local development of this repository only:

```kotlin
implementation(project(":diverge-sdk"))
```

Prefer version pins / GitHub Releases — do not track `main`. Version stays at **0.1.0** until the API is stable.

## License

[Apache-2.0](LICENSE.md) — Copyright © 2026 Diverge
