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
    implementation("ai.askdiverge:diverge-sdk:1.0.0")
}
```

Coordinates: group `ai.askdiverge`, artifact `diverge-sdk`, from [Maven Central](https://central.sonatype.com/).

For local development of this repository only:

```kotlin
implementation(project(":diverge-sdk"))
```

Prefer version pins / GitHub Releases — do not track `main`. Releases follow SemVer: `1.x.y` keeps the
public API and wire contract stable; patch versions fix, minor versions add, a major version is the only
place a breaking change may land.

## Usage

The SDK renders the conversation and nothing else — the host supplies the session token and data
lifecycle, and owns how the chat is presented and dismissed.

```kotlin
// Your backend calls, each returning a Result.
class HostChatbotCallbacks(private val session: VisitorSession) : ChatbotCallbacks {
    override suspend fun fetchToken(): Result<String> = session.currentToken()
    override suspend fun createNewToken(): Result<String> = session.renewToken()
    override suspend fun deleteChatData(): Result<Unit> = session.deleteData()
}

if (isChatOpen) {
    ChatbotBottomSheet(
        chatbotCallbacks = remember { HostChatbotCallbacks(session) },
        onDismiss = { isChatOpen = false }
    )
}
```

Every parameter is documented in the KDoc shipped with the artifact.

**`environment`** — which Diverge backend the SDK talks to. Defaults to `DivergeEnvironment.PRODUCTION`;
a host that says nothing keeps talking to the live API. Use `DivergeEnvironment.DEVELOPMENT` to integrate
against backend changes before they are released. The SDK owns the URLs for both.

```kotlin
environment = DivergeEnvironment.DEVELOPMENT
```

**`attachments`** — whether the visitor may pick photos: in the composer, to send to the
assistant or a live agent, on the card shown when the assistant asks for a photo, and in in-chat
forms. Defaults to `ChatbotAttachments.DISABLED`, which also hides that card. Pass
`ChatbotAttachments.PHOTO_LIBRARY` to offer them through the Android Photo Picker (no storage
permission). Read once, when the chat's ViewModel is created, so changing it while the sheet's
ViewModel is alive has no effect.

```kotlin
attachments = ChatbotAttachments.PHOTO_LIBRARY
```

## Versioning

Root [`VERSION`](VERSION). After changing it:

```bash
./scripts/sync-version.sh
./scripts/check-version.sh
```

Push a SemVer tag (`v1.0.0`) for a GitHub Release / Maven Central version. Patch for fixes (`1.0.1`),
minor for additive features (`1.1.0`), major for a breaking public-API or wire-contract change (`2.0.0`).
The SDK sends its version to the backend on every call, so the tag and `VERSION` must agree.

## License

[Apache-2.0](LICENSE.md) — Copyright © 2026 Diverge
