# TalkBack test checklist (Android)

Use this process before each release that changes UI surfaced by the SDK.
Also complete [`wcag-2.1-aa-checklist.md`](wcag-2.1-aa-checklist.md).
Reference: https://developer.android.com/guide/topics/ui/accessibility/testing

## Scope

The chat the SDK owns: `ChatbotBottomSheet` and everything inside it — header controls, the
conversation (assistant, visitor and agent bubbles, product and suggestion cards, forms, livechat
notes), the composer and its photo strip, the jump-to-newest control, Privacy & Data, the close
rating and the livechat rating (CSAT). Host screens, including the `sample`'s token screen, are
out of scope.

## Engineering baseline

| Area | How it is met |
|------|---------------|
| Labels | Every control has a `contentDescription` or visible text; decorative images use `null` |
| Grouping | A pending photo is one element (image label, position) with a "Remove photo" action; a suggestion card says it sends a message |
| Touch targets | ≥ 48 dp, including the photo's remove control (48 dp corner target) |
| Text scaling | Text from `MaterialTheme.typography` in `sp`; prices and titles wrap rather than clip |
| Live updates | Privacy & Data notices sit in a polite live region |
| Copy | Every string in `res/values` and the 15 locale folders |

## Setup (device)

1. Install and run the `sample` app with a visitor token.
2. Settings → Accessibility → TalkBack → On.
3. Prefer a physical device. On the emulator, gestures and keys injected with `adb` do not drive
   TalkBack reliably (injected taps activate controls instead of exploring them), so walk it by
   hand.

## Gestures to exercise

- [ ] Swipe through the header: title, Talk to a person / End live chat, Reset conversation
- [ ] Swipe through a conversation with products: each card reads brand, name, price and the
      struck price
- [ ] Composer: text field, Privacy Policy, Attach a photo, Send message; with a photo attached,
      the photo reads "Image, 1" and its actions offer "Remove photo"
- [ ] Jump-to-newest is announced and moves to the newest message
- [ ] Privacy & Data: each row, and the saved / failed notice is announced after a download
- [ ] Close rating and CSAT: scores read "1 out of 5" … "5 out of 5"; Skip and Send
- [ ] Livechat: the queued and joined notes, an agent's message with its name, typing indicator

## Device sign-off

| Build / version | Tester | Date | Device | Pass? | Notes |
|-----------------|--------|------|--------|-------|-------|
| 0.1.0 | code baseline (static + automated) | 2026-08-17 | — | Partial | `DivergeStatusView`, since removed |
| 1.1.0 | release QA (automated) | 2026-11-21 | Emulator, Android 16 | Partial | Accessibility tree checked on the chat, livechat and Privacy screens: every clickable control labelled, all targets ≥ 48 dp. TalkBack on: focus outline shown; gesture walk-through still owed by a person |
| | | | | | |
