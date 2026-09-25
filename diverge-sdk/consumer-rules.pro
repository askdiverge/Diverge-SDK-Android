# Consumer ProGuard / R8 rules shipped with the AAR.
#
# Only the types the SDK deliberately promises as public API belong here.
# Everything else is `internal`, or is referenced directly from consumer code
# and therefore kept by R8 through ordinary reachability.
#
# The Moshi DTOs in ai.askdiverge.data.model.** are NOT listed here: they carry
# @Keep, which androidx.annotation honours via its own bundled
# META-INF/proguard/androidx-annotations.pro.
#
# The @Composable entry points (ChatbotScreen / ChatbotBottomSheet) are also not
# listed: consumers call them directly, so keeping them by name would only stop
# R8 from shrinking the unused one out of the consumer's app.
#
# :sample:verifyR8PublicApiKeeps asserts this list stays in sync with the code.

-keep class ai.askdiverge.ChatbotCallbacks { public *; }
-keep class ai.askdiverge.domain.exception.ChatbotException { public *; }
-keep class ai.askdiverge.domain.exception.ChatbotException$* { public *; }
