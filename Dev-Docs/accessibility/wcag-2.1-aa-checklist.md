# WCAG 2.1 AA checklist (SDK-owned UI)

Target: [WCAG 2.1 Level AA](https://www.w3.org/WAI/WCAG21/quickref/?levels=aa) for UI the Diverge SDK owns
(for example ``DivergeStatusView`` / sample configure flows). Host chrome is out of scope.

Use alongside [VoiceOver](voiceover-checklist.md) and [TalkBack](talkback-checklist.md) checklists.

## Perceivable

- [x] Text alternatives: images/icons that convey meaning have labels; decorative content is hidden from AT — *v0.1.0 code baseline: status UI is text-only; no meaningful images*
- [x] Color is not the only means of conveying state (errors also use text) — *sample shows error text; status uses explicit “Not configured” copy*
- [ ] Contrast: normal text ≥ 4.5:1, large text ≥ 3:1 against background — *device/visual measurement still required*
- [x] Text can resize to 200% without loss of content (Dynamic Type / fontScale) — *iOS sample uses `.dynamicTypeSize`; Android uses `sp` text sizes*
- [x] Reflow: no horizontal scrolling required at 320 CSS px / small phone widths for SDK screens — *status + sample are vertical `ScrollView` / `ScrollView` stacks*

## Operable

- [x] All interactive controls reachable and activatable via VoiceOver / TalkBack — *code exposes labels/hints; device confirmation still listed below*
- [x] Touch targets ≥ ~44×44 pt (iOS) / ~48×48 dp (Android) for SDK buttons — *sample configure button `minHeight` 48; iOS button `minHeight: 48`*
- [x] Focus order is logical; no keyboard/AT traps in SDK modals — *v0.1.0 has no SDK modals*
- [x] Motion: no content that flashes more than three times per second — *no animated/flashing SDK UI*

## Understandable

- [x] Labels and instructions identify inputs (API key field, configure button)
- [x] Error messages identify the field and how to fix (e.g. blank API key) — *localized “API key must not be blank.”*
- [x] Consistent naming for environment / version status — *shared `accessibilityDump` contract iOS/Android*

## Robust

- [x] Controls expose correct roles/traits (button, header, text field) — *SwiftUI header traits + Android button/TextInput*
- [x] State changes (configured / not configured) are announced or visible to AT — *status copy + content descriptions; dumps asserted in unit tests*

## Automated coverage (CI)

- iOS: `DivergeStatusViewSnapshotTests` asserts the accessibility dump contract (exact string equality).
- Android: `DivergeTest.statusViewDump*` asserts dump contract keys/values (no API key leakage).

## Sign-off

| Build / version | Tester | Date | Pass? | Notes |
|-----------------|--------|------|-------|-------|
| 0.1.0 | code baseline (static + automated) | 2026-08-11 | Pass (partial) | Device VoiceOver/TalkBack + contrast measurement still owed |
|                 |        |      |       |       |

> Full AA certification of unfinished product UI (PDP, checkout, etc.) is deferred until those surfaces ship.
>
> **Still owed on device before calling a11y “done”:** contrast measurement, VoiceOver/TalkBack smoke from the paired checklists.
