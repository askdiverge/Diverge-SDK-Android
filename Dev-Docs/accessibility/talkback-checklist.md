# TalkBack test checklist (Android)

Use this process before each release that changes UI surfaced by the SDK.
Also complete [`wcag-2.1-aa-checklist.md`](wcag-2.1-aa-checklist.md).
Reference: https://developer.android.com/guide/topics/ui/accessibility/testing

## Scope (v0.1)

Applies to programmatic `DivergeStatusView` and the `sample` configure flow only.

## Implementation status (engineering)

| Area | Status |
|------|--------|
| Process + this checklist | Done |
| Content descriptions on StatusView + sample | Done |
| Accessibility heading on title; live region on status/env + error | Done |
| AA-safe text colors (primary `#1A1A1A`, secondary `#4A4A4A`) | Done |
| Persistent error `TextView` (not Toast-only) | Done |
| Touch targets ≥ 48 dp | Done |
| Unit dump + Paparazzi goldens | Done |
| Physical-device TalkBack gesture sign-off | Operator — fill table below |

## Legend

| Mark | Meaning |
|------|---------|
| Code baseline | Covered by content descriptions in source and/or unit tests |
| Device owed | Requires physical-device (or emulator TalkBack) sign-off below |

## Setup (device owed)

1. Install/run the `sample` app (`./gradlew :sample:installDebug` or Android Studio).
2. Settings → Accessibility → TalkBack → On.
3. Prefer a physical device; emulator TalkBack is acceptable for smoke tests.

## Smoke checks

- [x] All interactive controls receive focus and have content descriptions — *code baseline*
- [x] Decorative images have empty/`null` content descriptions or are important=false — *no images in v0.1.0 SDK UI*
- [x] Focus order matches visual order — *vertical LinearLayout; confirm on device*
- [x] Custom views expose correct AccessibilityNodeInfo roles/actions — *standard widgets + heading*
- [x] Live regions announce loading and error updates — *status env live region + error TextView*
- [x] Dialogs trap focus appropriately and restore on dismiss — *N/A for v0.1.0*
- [x] Touch target sizes meet ~48dp guidelines — *configure button + API key field `minHeight` 48dp*

## Gestures to exercise (device owed)

Run once per release that touches SDK UI; then update the sign-off table.

- [ ] Swipe right/left through: title → instructions → API key → Configure → (error if any) → StatusView children
- [ ] Double-tap **Configure sandbox** with a blank key; confirm error TextView is spoken
- [ ] Double-tap **Configure sandbox** with `sk_sandbox_demo`; confirm StatusView speaks version + environment + URL
- [ ] Explore-by-touch — *optional for v0.1 sparse layout*

## Device sign-off

| Build / version | Tester | Date | Device | Pass? | Notes |
|-----------------|--------|------|--------|-------|-------|
| 0.1.0 | code baseline (static + automated) | 2026-08-17 | — | Partial | Gestures still need a human AT pass |
| | | | | | |
