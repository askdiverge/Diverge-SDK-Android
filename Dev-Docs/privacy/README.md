# PrivacyInfo.xcprivacy guidance

The template [`PrivacyInfo.xcprivacy.template`](PrivacyInfo.xcprivacy.template) and the
**shipped** package resource [`Sources/DivergeSDK/PrivacyInfo.xcprivacy`](../../Sources/DivergeSDK/PrivacyInfo.xcprivacy)
both start with **empty** accessed-API and collected-data arrays on purpose.

Before releasing the iOS SDK:

1. Audit the SDK binary / source for required-reason APIs (file timestamps, UserDefaults, disk space, boot time, etc.).
2. Add only the categories you actually use, with the correct reason codes from Apple’s list.
3. Keep the package resource `PrivacyInfo.xcprivacy` in sync with the template (or generate from one source).
4. Do **not** declare APIs “just in case” — incorrect declarations can fail App Store review.

Host apps still own ATT prompts and their own Info.plist usage strings — see the usage-description template and [`Docs/site/att.html`](../../Docs/site/att.html).
