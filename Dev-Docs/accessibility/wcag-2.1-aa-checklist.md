# WCAG 2.1 AA checklist (Android SDK-owned UI)

Target: [WCAG 2.1 Level AA](https://www.w3.org/WAI/WCAG21/quickref/?levels=aa) for UI the Diverge SDK owns:
`ChatbotBottomSheet` and its content. Host chrome is out of scope.

Use alongside [`talkback-checklist.md`](talkback-checklist.md).

iOS VoiceOver / WCAG live in [Diverge-SDK-iOS Docs/accessibility](https://github.com/askdiverge/Diverge-SDK-iOS/tree/main/Docs/accessibility).

## Colour

Most chat colours come from the chatbot's `/config` theme (set in the Diverge dashboard), with the
host's Material 3 scheme as the fallback. Contrast therefore depends on each chatbot's
configuration and is checked there; the SDK does not change configured colours.

## Perceivable

- [x] Text alternatives: images in messages and cards have labels; decorative glyphs have none
- [x] Colour is not the only means of conveying state: errors and notices are text; discounted
      prices are also struck through
- [ ] Contrast ≥ 4.5:1 for the configured theme — *per chatbot, in the dashboard*
- [x] Text resizes with the system font scale (`sp`); checked at 1.0, 1.15 and 2.0 on a 360 dp
      wide screen: prices wrap whole, nothing clips
- [x] Reflow at small widths: product prices move to their own line rather than truncate

## Operable

- [x] Controls reachable and labelled (accessibility tree checked on device)
- [x] Touch targets ≥ 48×48 dp
- [x] No traps in SDK dialogs: rating, CSAT, leave/end confirmations and Privacy & Data close with
      back
- [x] No flashing content; the streaming reply border and the typing indicator hold still when
      the system's animations are off

## Understandable / Robust

- [x] Inputs have labels; form errors say what to fix ("Enter a valid email address")
- [x] Roles: buttons, headings and custom actions ("Remove photo") exposed
- [ ] TalkBack gesture walk-through — *owed, see the TalkBack checklist*

## Device sign-off

| Build / version | Tester | Date | Pass? | Notes |
|-----------------|--------|------|-------|-------|
| 0.1.0 | code baseline + calculated contrast | 2026-08-17 | Partial | `DivergeStatusView`, since removed |
| 1.1.0 | release QA on emulator (automated) | 2026-11-21 | Partial | Font scale and narrow-width reflow checked; TalkBack walk-through owed |
| | | | | |
