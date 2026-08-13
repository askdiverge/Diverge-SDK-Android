# VoiceOver test checklist (iOS)

Use this process before each release that changes UI surfaced by the SDK.
Also complete [`wcag-2.1-aa-checklist.md`](wcag-2.1-aa-checklist.md).
Reference: https://developer.apple.com/documentation/accessibility/supporting-voiceover-in-your-app

## Setup

- [ ] Enable VoiceOver on a physical device (Settings → Accessibility → VoiceOver)
- [ ] Confirm rotor includes Headings, Links, Form Controls, Containers
- [ ] Use the sample app or host integration that embeds SDK UI

## Smoke checks

- [x] All interactive controls are focusable and have spoken labels — *code baseline: sample TextField/Button labels + hints*
- [x] Images that convey meaning have accessibility labels; decorative images are hidden — *no images in v0.1.0 SDK UI*
- [x] Dynamic type / large content sizes do not clip critical text — *sample `.dynamicTypeSize(.small ... .accessibility3)`*
- [x] Focus order follows visual reading order — *single vertical stack; device confirm still owed*
- [x] Modals/sheets move VoiceOver focus into the dialog and restore on dismiss — *N/A for v0.1.0 (no SDK sheets)*
- [x] Loading and error states are announced — *configure errors shown as labeled text*
- [x] Custom controls expose traits (button, selected, etc.) correctly — *SwiftUI Button + header traits on status title*

## Gestures to exercise

- [ ] Swipe right/left through the full screen hierarchy
- [ ] Double-tap to activate primary actions
- [ ] Escape / two-finger Z to dismiss sheets where applicable

## Sign-off

| Build / version | Tester | Date | Pass? | Notes |
|-----------------|--------|------|-------|-------|
| 0.1.0 | code baseline (static + automated) | 2026-08-11 | Pass (partial) | Device gesture rows above still blank |
|                 |        |      |       |       |
