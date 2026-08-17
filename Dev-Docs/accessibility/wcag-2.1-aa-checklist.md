# WCAG 2.1 AA checklist (Android SDK-owned UI)

Target: [WCAG 2.1 Level AA](https://www.w3.org/WAI/WCAG21/quickref/?levels=aa) for UI the Diverge SDK owns
(`DivergeStatusView` / sample configure flow). Host chrome is out of scope.

Use alongside [`talkback-checklist.md`](talkback-checklist.md).

## Scope (v0.1)

v0.1 targets StatusView + sample only. Full AA for unfinished product UI is deferred until those surfaces ship.

iOS VoiceOver / WCAG live in [Diverge-SDK-iOS Docs/accessibility](https://github.com/askdiverge/Diverge-SDK-iOS/tree/main/Docs/accessibility).

## Perceivable

- [x] Text alternatives — *status UI is text-only*
- [x] Color is not the only means of conveying state — *errors use text / Toast*
- [ ] Contrast: normal text ≥ 4.5:1, large text ≥ 3:1 — ***device/visual measurement owed***
- [x] Text can resize with fontScale — *`sp` text sizes*
- [x] Reflow at small widths — *vertical `ScrollView`*

## Operable

- [x] Controls reachable via TalkBack — *content descriptions in code; device confirm on TalkBack checklist*
- [x] Touch targets ≥ ~48×48 dp — *sample configure controls*
- [x] No AT traps in SDK dialogs — *none in v0.1*
- [x] No flashing content

## Understandable / Robust

- [x] Labels identify inputs; blank API key error is clear
- [x] Roles/actions correct; configured state described
- [x] Dump contract covered in unit tests; Paparazzi goldens cover StatusView pixels

## Device sign-off

| Build / version | Tester | Date | Pass? | Notes |
|-----------------|--------|------|-------|-------|
| 0.1.0 | code baseline | 2026-08-11 | Partial | Contrast + TalkBack device still owed |
| | | | | |
