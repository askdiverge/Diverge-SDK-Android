# TalkBack test checklist (Android)

Use this process before each release that changes UI surfaced by the SDK.
Also complete [`wcag-2.1-aa-checklist.md`](wcag-2.1-aa-checklist.md).
Reference: https://developer.android.com/guide/topics/ui/accessibility/testing

## Setup

- [ ] Enable TalkBack (Settings → Accessibility → TalkBack)
- [ ] Prefer a physical device; emulator TalkBack is acceptable for smoke tests
- [ ] Use the sample app (`android/sample`) or a host integration

## Smoke checks

- [x] All interactive controls receive focus and have content descriptions — *sample sets contentDescription; status `bind` sets descriptions*
- [x] Decorative images have empty/`null` content descriptions or are important=false — *no images in v0.1.0 SDK UI*
- [x] Focus order matches visual order — *vertical LinearLayout; device confirm still owed*
- [x] Custom views expose correct AccessibilityNodeInfo roles/actions — *standard TextView/Button/TextInput; status is LinearLayout of TextViews*
- [x] Live regions announce loading and error updates — *errors via Toast + status text; no indefinite loaders in v0.1.0*
- [x] Dialogs trap focus appropriately and restore on dismiss — *N/A for v0.1.0*
- [x] Touch target sizes meet ~48dp guidelines for SDK-owned controls — *configure button + API key field `minHeight` 48dp*

## Gestures to exercise

- [ ] Swipe right/left through the hierarchy
- [ ] Double-tap to activate
- [ ] Explore-by-touch on dense product UI if applicable — *N/A until product UI ships*

## Sign-off

| Build / version | Tester | Date | Pass? | Notes |
|-----------------|--------|------|-------|-------|
| 0.1.0 | code baseline (static + automated) | 2026-08-11 | Pass (partial) | Device gesture rows above still blank |
|                 |        |      |       |       |
