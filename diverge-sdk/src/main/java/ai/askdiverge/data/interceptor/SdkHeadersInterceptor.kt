package ai.askdiverge.data.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Identifies the calling SDK (platform and version) and lists the features it requests on every
 * request. It is installed next to [BearerTokenInterceptor] on the authenticated client only, so
 * these headers travel exactly where the bearer token does. The API major the client speaks is the
 * `/api/v1/` path prefix.
 *
 * @param sdkVersion SemVer of the SDK release, from `VERSION`.
 * @param clientProfiles every feature this build requests. The backend grants the subset the
 * caller is eligible for.
 */
internal class SdkHeadersInterceptor(
    private val sdkVersion: String,
    clientProfiles: List<ClientProfile>
) : Interceptor {

    private val clientProfileHeaderValue = ClientProfile.headerValue(clientProfiles)

    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder()
            .header(SDK_PLATFORM_HEADER, SDK_PLATFORM)
            .header(SDK_VERSION_HEADER, sdkVersion)
            .header(CLIENT_PROFILE_HEADER, clientProfileHeaderValue)
            .build()
    )

    companion object {

        /**
         * Which client implementation is calling. Together with [SDK_VERSION_HEADER] this
         * identifies one release of one SDK, since the iOS, Android and web SDKs version
         * independently.
         */
        const val SDK_PLATFORM_HEADER = "X-Diverge-SDK-Platform"

        /** The value this repository sends for [SDK_PLATFORM_HEADER]. */
        const val SDK_PLATFORM = "android"

        /** SemVer of the SDK release, from `VERSION`. */
        const val SDK_VERSION_HEADER = "X-Diverge-SDK-Version"

        /** The features the build requests, as a comma-separated list; see [ClientProfile]. */
        const val CLIENT_PROFILE_HEADER = "X-Diverge-Client-Profile"
    }
}
