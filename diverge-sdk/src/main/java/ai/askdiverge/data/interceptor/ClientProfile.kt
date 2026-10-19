package ai.askdiverge.data.interceptor

/**
 * A chat feature this SDK build requests from the backend. The build sends every profile it
 * requests on each authenticated call, as a comma-separated list in
 * [SdkHeadersInterceptor.CLIENT_PROFILE_HEADER].
 *
 * The backend grants a subset of the list and registers the assistant's tools for the granted
 * profiles only, so one conversation can combine several features while the assistant reaches
 * only for features this build has UI for. Eligibility is the backend's decision: it can grant or
 * withhold a profile per SDK platform and version or per account behind the token, and it ignores
 * values it does not know, so a newer build keeps working against an older backend. Declaring
 * features directly keeps that rule a membership check; a rule derived from
 * [SdkHeadersInterceptor.SDK_VERSION_HEADER] would need a version range per platform, widened on
 * every release.
 *
 * A new entry is added when the SDK ships UI for a new feature (for example customer service with
 * livechat and forms). Wire values are a contract shared with the backend and the other SDKs; each
 * value stays bound to the feature it was introduced for.
 *
 * @property wireValue the string sent in the header.
 */
internal enum class ClientProfile(val wireValue: String) {

    /**
     * Conversational search with product cards. The backend registers the product search and
     * recommendation tools for it.
     */
    PRODUCT_RECOMMENDATION("product-recommendation");

    companion object {

        /**
         * The wire form of the requested [profiles]: each value once, in declaration order, joined
         * with `", "`. The order is canonical so the same request always produces the same header.
         */
        fun headerValue(profiles: Collection<ClientProfile>): String =
            entries
                .filter { it in profiles }
                .joinToString(separator = ", ") { it.wireValue }
    }
}
