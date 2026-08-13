# Consumer ProGuard / R8 rules shipped with the AAR.
# Scoped to public SDK types only (not ai.askdiverge.sdk.**).

-keep class ai.askdiverge.sdk.Diverge { public *; }
-keep class ai.askdiverge.sdk.Configuration { public *; }
-keep class ai.askdiverge.sdk.Environment { public *; }
-keepclassmembers enum ai.askdiverge.sdk.Environment { *; }
-keep class ai.askdiverge.sdk.DivergeClient { public *; }
-keep class ai.askdiverge.sdk.DivergeException { public *; }
-keep class ai.askdiverge.sdk.DivergeException$* { public *; }
-keep class ai.askdiverge.sdk.DivergeStatusView { public *; }
