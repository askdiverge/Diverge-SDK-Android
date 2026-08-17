# WCAG 2.1 AA checklist (Android SDK-owned UI)

Target: [WCAG 2.1 Level AA](https://www.w3.org/WAI/WCAG21/quickref/?levels=aa) for UI the Diverge SDK owns
(`DivergeStatusView` / sample configure flow). Host chrome is out of scope.

Use alongside [`talkback-checklist.md`](talkback-checklist.md).

## Scope (v0.1)

v0.1 targets StatusView + sample only. Full AA for unfinished product UI is deferred until those surfaces ship.

iOS VoiceOver / WCAG live in [Diverge-SDK-iOS Docs/accessibility](https://github.com/askdiverge/Diverge-SDK-iOS/tree/main/Docs/accessibility).

## Contrast (calculated)

| Pair | Ratio | AA normal text (≥ 4.5:1) |
|------|-------|---------------------------|
| `#1A1A1A` on `#FFFFFF` | ≈ 17.4:1 | Pass |
| `#4A4A4A` on `#FFFFFF` | ≈ 8.9:1 | Pass |
| `#1A1A1A` on `#F7F5F1` | ≈ 16.0:1 | Pass |
| `#4A4A4A` on `#F7F5F1` | ≈ 8.1:1 | Pass |
| `#8B0000` on `#1FFF0000` wash | ≥ 4.5:1 | Pass |

Re-run if `COLOR_PRIMARY` / `COLOR_SECONDARY` or sample colors change.

## Perceivable

- [x] Text alternatives — *status UI is text-only*
- [x] Color is not the only means of conveying state — *errors use persistent text (+ Toast)*
- [x] Contrast: normal text ≥ 4.5:1, large text ≥ 3:1 — *fixed AA-safe palette above*
- [x] Text can resize with fontScale — *`sp` text sizes*
- [x] Reflow at small widths — *vertical `ScrollView`*

## Operable

- [x] Controls reachable via TalkBack — *content descriptions / headings; device confirm on TalkBack checklist*
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
| 0.1.0 | code baseline + calculated contrast | 2026-08-17 | Partial | TalkBack gestures still owed on device |
| | | | | |
