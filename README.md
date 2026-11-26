# Diverge SDK for Android

Open-source ecommerce SDK for **Android**.

> iOS SDK: [Diverge-SDK-iOS](https://github.com/askdiverge/Diverge-SDK-iOS)

## Requirements

| | |
|--|--|
| minSdk | **24** |
| compileSdk | 37 |
| Kotlin | 2.2+ |
| JDK | 17 |

## Installation

```kotlin
dependencies {
    implementation("ai.askdiverge:diverge-sdk:1.1.0")
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

**`rating`** — whether dismissing the sheet may offer a conversation rating (`POST /chat/rate`)
after the visitor has sent at least one message. Defaults to `ChatbotRating.ENABLED`. Pass
`ChatbotRating.DISABLED` when the host must close immediately without that prompt. Livechat CSAT
still owns the close when that feedback prompt applies. Read once, when the chat's ViewModel is
created, so changing it while the sheet's ViewModel is alive has no effect.

```kotlin
rating = ChatbotRating.DISABLED
```

**`onLivechatSessionChange`** — optional. Once the visitor's livechat session has been read, the
SDK calls this with it (`LivechatSessionInfo`: inactive / waiting / active, with the agent's name /
closed) on each presentation of the sheet, then on every change while the sheet is on screen. Use
it for host badges or to offer reopening a chat closed mid-session. Nothing is delivered while the
sheet is hidden, where the poller stays parked. Treat the value as the current state, not an event:
it can be delivered again unchanged (for example after a configuration change), and rapid
intermediate changes may be skipped. Pass it by name, as below, not as a trailing lambda.

```kotlin
onLivechatSessionChange = { info ->
    // e.g. badge the launcher with info.status / info.agentDisplayName
}
```

### Session metadata

**`sessionMetadata`** — optional. Attach what your CS systems need to identify the visitor, the
same way the web widget's `setMetadata` and `setSignedMetadata` do. Create one
`ChatbotSessionMetadata` when the app starts, keep it next to your `ChatbotCallbacks`, and pass it to
every sheet. Call it from any thread at any time, before the chat is shown too; calls are sent in
order while a chat is on screen with a session, and wait in memory for the next chat shown
otherwise. Read once, when the chat's ViewModel is created.

```kotlin
val sessionMetadata = ChatbotSessionMetadata()

sessionMetadata.setMetadata(
    mapOf(
        "storeSlug" to "boozt-se",
        "languageCode" to "sv",
        "platformName" to "android",
        "screen" to currentScreen.id
    )
)

ChatbotBottomSheet(
    chatbotCallbacks = callbacks,
    onDismiss = { isChatOpen = false },
    sessionMetadata = sessionMetadata
)
```

Values are `String`, `Boolean`, `Int`, `Long`, `Float`, `Double`, `null`, and `List` or
`Map<String, *>` of those, nested at most 20 levels deep. A call the backend would refuse whole is
dropped on its own and logged through `ChatbotLogger` without its values: one with any other
value, a NaN or infinite number, deeper nesting, a top-level key `verified_metadata` or
`signed_metadata`, or the key `__proto__`, `prototype` or `constructor` at any depth. Repeated
calls deep-merge into the session's metadata. It comes from the device, so treat it as untrusted.
The backend reads numbers as doubles, so send an id above 2^53 as a `String`, not a `Long`.
For trusted values, have your backend sign an RS256 JWT (`iat` and `exp` at most 15 minutes apart)
with the key you registered for the chatbot, and pass it on:

```kotlin
sessionMetadata.setSignedMetadata(myAuth.signedChatMetadata())
```

Its claims are verified and stored as verified metadata, and actions use them only while the newest
token is unexpired. A token has to be unexpired when it reaches the backend, and it is sent only
while a chat is on screen, so sign one on login, again as you present the chat, and on a timer
shorter than its lifetime while the chat is open. Of the tokens set while no chat is shown, the
newest one is sent. A token the backend refuses is dropped.

When the visitor signs out, call `sessionMetadata.clear()` before your `sessionInvalidations()`
emits, so nothing set for that visitor reaches the next one's session. After the visitor deletes
their data, anything not yet sent is dropped too. A conversation reset starts a new session without
the metadata the old one got, so call `setMetadata` and `setSignedMetadata` again from your
`createNewToken()` or later: calls made during a reset wait and go to the new session.

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
