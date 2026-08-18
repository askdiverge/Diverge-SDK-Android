# Consumer ProGuard / R8 rules shipped with the AAR.
# Scoped to public SDK types only (not ai.askdiverge.sdk.**).

-keep class ai.askdiverge.sdk.Diverge { public *; }
-keep class ai.askdiverge.sdk.DivergeConfiguration { public *; }
-keep class ai.askdiverge.sdk.DivergeEnvironment { public *; }
-keepclassmembers enum ai.askdiverge.sdk.DivergeEnvironment { *; }
-keep class ai.askdiverge.sdk.DivergeClient { public *; }
-keep class ai.askdiverge.sdk.DivergeException { public *; }
-keep class ai.askdiverge.sdk.DivergeException$* { public *; }
-keep class ai.askdiverge.sdk.DivergeStatusView { public *; }
