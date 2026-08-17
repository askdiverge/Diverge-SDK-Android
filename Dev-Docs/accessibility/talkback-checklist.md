# TalkBack test checklist (Android)

Use this process before each release that changes UI surfaced by the SDK.
Also complete [`wcag-2.1-aa-checklist.md`](wcag-2.1-aa-checklist.md).
Reference: https://developer.android.com/guide/topics/ui/accessibility/testing

## Scope (v0.1)

Applies to programmatic `DivergeStatusView` and the `sample` configure flow only.

## Legend

| Mark | Meaning |
|------|---------|
| Code baseline | Covered by content descriptions in source and/or unit tests |
| Device owed | Requires physical-device (or emulator TalkBack) sign-off below |

## Setup (device owed)

- [ ] Enable TalkBack (Settings → Accessibility → TalkBack)
- [ ] Prefer a physical device; emulator TalkBack is acceptable for smoke tests
- [ ] Use the sample app (`sample/`) or a host integration

## Smoke checks

- [x] All interactive controls receive focus and have content descriptions — *code baseline: sample sets contentDescription; status `bind` sets descriptions*
- [x] Decorative images have empty/`null` content descriptions or are important=false — *no images in v0.1.0 SDK UI*
- [x] Focus order matches visual order — *code baseline: vertical LinearLayout; **device owed** to confirm*
- [x] Custom views expose correct AccessibilityNodeInfo roles/actions — *standard widgets*
- [x] Live regions announce loading and error updates — *errors via Toast + status text*
- [x] Dialogs trap focus appropriately and restore on dismiss — *N/A for v0.1.0*
- [x] Touch target sizes meet ~48dp guidelines — *configure button + API key field `minHeight` 48dp*

## Gestures to exercise (device owed)

- [ ] Swipe right/left through the hierarchy
- [ ] Double-tap to activate
- [ ] Explore-by-touch on dense product UI if applicable — *N/A until product UI ships*

## Device sign-off

| Build / version | Tester | Date | Device | Pass? | Notes |
|-----------------|--------|------|--------|-------|-------|
| 0.1.0 | code baseline (static + automated) | 2026-08-11 | — | Partial | Gesture rows still blank |
| | | | | | |
