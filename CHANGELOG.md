# Changelog — Diverge SDK Android

## [Unreleased]

### Added

- `ChatbotBottomSheet(onLivechatSessionChange = …)` and `LivechatSessionInfo`: once the session
  has been read, the host hears whether the visitor is queued, talking to an agent (with the
  agent's name) or the session has ended, on each presentation of the sheet and on every change
  while it is on screen, so it can show the agent in its own UI and offer to reopen a chat the
  visitor closed mid-session. Default is a no-op; nothing is delivered while the sheet is hidden.
  The value is the current state, not an event: it can be delivered again unchanged, for example
  after a configuration change, and rapid intermediate changes may be skipped. Pass it by name
- In-chat banners: after connect and on each open of the sheet, `GET /chat/banners?url=` (the
  `currentPage` string the sheet was opened with) paints sticky promo strips under the top bar,
  scrolling past a quarter of its height. CTA opens absolute http(s) links; dismissed banners stay
  hidden for the ViewModel lifetime, reconnects included. An open from another page clears the
  strip until that page's banners arrive. A failed request, a 401 included, leaves the strip as it
  was and never ends the session
- Download my data: Privacy & Data gains a row that opens the system save dialog and streams
  `GET /chat/export` into the JSON document the visitor creates there, then confirms the save; the
  SDK keeps no copy, and a failed or cancelled download deletes its partial file unless the storage
  provider refuses or the app is killed first. The visitor token is kept; delete remains the host
  `ChatbotCallbacks.deleteChatData` hook. Hosts need no extra setup
- Privacy note for hosts: with Download my data the chat lets a visitor save an export of their
  data to a document they choose on the device; the SDK itself keeps no copy
- Livechat agent avatars: `role: agent` text bubbles show the agent's photo from the message
  identity (`avatar_url`, relative paths resolved against the API base), or a person icon when the
  URL is missing or fails to load—not the `/config` assistant avatar. Assistant turns keep the
  session avatar. Until the backend sends `agent` on history messages, agent turns loaded from
  history show the person icon
- Suggestion cards: a `suggestions` message part (history and stream `start_part` /
  `append_suggestion`) renders a two-column card grid; tapping a card sends its `prompt_text` as
  the next user message without clearing the composer draft
- Conversation close rating: after the visitor has sent a message, dismissing the sheet may offer a
  1–5 rating (`POST /chat/rate`; optional comment for 1–3) before `onDismiss`. Defaults to
  `ChatbotRating.ENABLED` on `ChatbotBottomSheet`; pass `DISABLED` to dismiss immediately. Skip
  dismisses without posting and is not asked again, unless a score was picked and not stored (on
  the comment step or after a failed send), which a later dismiss asks for again; back returns to
  the chat. Livechat CSAT still owns the close when that prompt applies
- `request_image_upload` marker: an Add-photo card in the conversation (history, stream `part`
  events and livechat messages) opens the shared Photo Picker when the host offers
  `PHOTO_LIBRARY`; the pick honours the marker's `max_size_bytes`, and a livechat session's
  smaller limit while one is open, including while queued. A marker whose `accepted_types` lacks
  `image/jpeg` is not shown. With `DISABLED` (the default) the marker is dropped entirely
- Composer photo attach: when the host offers `PHOTO_LIBRARY`, the visitor can pick photos and
  send them with or without text on the assistant stream or to a live agent
  (`ImageInput` parts on `/messages` and `/livechat/messages`): one per message to the assistant,
  which reads only a message's first image, and up to 8 to an agent. While an agent is connected,
  attach also requires `/config` `attachments_enabled`
- After a livechat session closes with feedback pending, a CSAT prompt asks for a 1–5 rating
  (`POST /livechat/feedback`); scores 1–3 can include an optional comment. Skip or dismiss posts
  nothing before a score is picked and posts a picked 1–3 score without its comment; the prompt
  can be dismissed while a rating is being sent, which then finishes in the background. A session
  found already closed when the chat starts is not asked; one that ends while the chat is hidden
  is asked when it is shown again. Any visitor-session expiry now also ends the livechat session
  in the SDK and drops the prompt
- While queued for a live agent, a configured `livechat_waiting` form from `/config` can be filled
  and saved (`GET /session`, `PATCH /forms/{id}/values`) so the agent sees the visitor's details
- After an in-chat form is submitted, the livechat state is read again, so a session the form's
  actions started on the server (queue note, end control, agent replies) shows without reopening
  the chat
- Livechat visitor typing and agent typing indicator: while an agent is connected, draft changes
  POST `is_typing` on `/livechat/typing` (once per burst, stopped on send, idle or pause; a failed
  post is tried at most 3 times and logged once); sync `is_agent_typing` drives the composer
  typing row, which clears when a sync fails
- Agent file and image message parts render in the conversation; temporary download links are
  re-signed from the livechat log on tap, and a link that cannot be opened shows a snackbar.
  Conversation images are kept in memory only, never in Coil's disk cache
- Livechat handover and conversation UI: header control to start or end a session, the
  `request_human_agent` card, session notes (queued / agent joined / ended), composer placeholders
  by status, and a leave/end confirmation
- Every livechat handover sends the agent a `client_context`: the device's language tag
  (`browser_language`), `os: "Android"`, and the `currentPage` the host passed
  (`current_page_url`, cut to 2,048 characters). Agents see it while the visitor waits; pass a
  `currentPage` that describes the screen, never the person
- While an agent is connected (`active`), visitor sends go to the agent over
  `POST /livechat/messages` instead of the AI stream; while queued (`waiting`) they still go to
  the assistant. While a session is open and the chat is on screen, the SDK polls
  `GET /livechat/sync` so agent replies appear in the conversation
- In-chat contact, support-ticket and custom forms (`show_contact_form` /
  `show_support_ticket` / `show_form`), gated by the `customer-service` client profile
- `attachments` parameter on `ChatbotBottomSheet`, taking `ChatbotAttachments`
  (`DISABLED` / `PHOTO_LIBRARY`). Defaults to `DISABLED`, so an existing host offers no photo
  attach until it opts in; a form with a required file field then shows as unavailable. Pass
  `PHOTO_LIBRARY` to let the composer, custom-form file fields and support-ticket photos use the
  Android Photo Picker (no storage permission). Read once, when the chat's ViewModel is created

### Changed

- A message part, rich-text block or table-cell block that fails to decode (a required field
  missing, or a value the SDK does not know, such as a new span style) is now logged and left out;
  the rest of the message and its conversation history still load. Previously one such item
  failed the whole message, and with it the history page. Hosts need no change
- `ChatbotBottomSheet`'s `onDismiss` now runs after the conversation-rating prompt when the
  visitor has sent a message, because `rating` defaults to `ChatbotRating.ENABLED`. Pass
  `rating = ChatbotRating.DISABLED` to keep the previous immediate dismiss
- Jump-to-newest sits in the trailing corner, is hidden while the chat has a single message and
  while the keyboard is up, and has a TalkBack label

### Fixed

- Welcome message (and privacy subtitle) stay visible when message history fails to load and no
  earlier messages are shown, instead of leaving a blank conversation with only a retry banner
- When message history fails to load because the visitor session has ended (HTTP 401), the chat
  now switches to the session-expired screen, whose Retry starts a new session; before, the inline
  “Couldn't load older messages” Retry kept failing
- Images and attachments from the server load only from `http`, `https` or `data:` URLs, whatever
  the case of the scheme; any other URL, such as `file:` or `content:`, is ignored instead of being
  read from the device
- A chat opened again with a different `currentPage` now sends that page with messages, form
  submissions and livechat handovers; before, it kept sending the page it was first opened with
- Product card prices are written for the device's locale and the product's currency, such as
  "249,50 kr." in Danish or "DKK 249.50" in English, instead of "249.5 DKK". When a price and its
  struck-through original price do not fit side by side, the original price moves to its own line
  unbroken; a price wraps only when it alone is wider than the card. Hosts need no change

## [1.0.0] - 2026-11-14

First release on Maven Central. Replaces the 0.1.0 surface; see
[`Dev-Docs/integration/v1.0.0.md`](Dev-Docs/integration/v1.0.0.md).

### Added

- Public API: `ChatbotBottomSheet`, the chat as a Compose modal bottom sheet; `ChatbotCallbacks`,
  the host's `suspend` token and delete-data hooks returning `Result`; `ChatbotException`;
  `ChatbotLogger`; `DivergeEnvironment`. Consumer R8 rules ship in the AAR
- `X-Diverge-SDK-Platform: android`, `X-Diverge-SDK-Version` and `X-Diverge-Client-Profile` on
  every authenticated API call (`/config`, `/messages`). Platform and version identify the
  calling SDK release; the profile header lists every feature the build requests
  (comma-separated, `product-recommendation` today), and the backend enables the tools for the
  ones it grants, so one chat can combine features. Image downloads carry only the request itself.
  Hosts need no change
- `environment` parameter on `ChatbotBottomSheet`, taking `DivergeEnvironment`
  (`PRODUCTION` / `DEVELOPMENT`), so a host can integrate against the development API.
  Defaults to `PRODUCTION`, so an existing host keeps talking to the live API without changes.
  The sample uses `DEVELOPMENT` in debug builds

### Changed

- The sample is a minimal Compose host — paste a session token, open the chat in a bottom sheet —
  replacing the removed `DivergeStatusView` screen
- While a reply streams, the assistant bubble draws a rotating gradient border in the `/config`
  `thinking_border_gradient` colours, and none when that list is empty. A finished assistant
  bubble has no border unless `/config` sets one

### Removed

- The 0.1.0 API in `ai.askdiverge.sdk`: `Diverge.configure` / `Diverge.shared`, `Configuration`,
  `DivergeClient`, `DivergeException`, `Environment` (`SANDBOX` / `PRODUCTION`) and
  `DivergeStatusView`. Use `ChatbotBottomSheet` with `ChatbotCallbacks`

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
