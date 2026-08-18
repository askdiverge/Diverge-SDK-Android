package ai.askdiverge.sample

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ai.askdiverge.sdk.DivergeConfiguration
import ai.askdiverge.sdk.Diverge
import ai.askdiverge.sdk.DivergeEnvironment

/**
 * Linkability smoke from the sample module.
 *
 * R8 survival of public API is proven by Gradle task `:sample:verifyR8PublicApiKeeps`
 * (inspects release `mapping.txt` after minify), not by this JVM unit test.
 */
class R8PublicApiSmokeTest {
    @Test
    fun publicApiSymbolsAreLinkable() {
        val configure = Diverge::class.java.getDeclaredMethod(
            "configure",
            DivergeConfiguration::class.java,
        )
        assertNotNull(configure)

        val client = Diverge.configure(
            DivergeConfiguration(apiKey = "sk_r8_smoke_test", environment = DivergeEnvironment.SANDBOX),
        )
        assertTrue(client.apiBaseUrl.startsWith("https://"))
        assertNotNull(Diverge.shared)
        Diverge.reset()
    }
}
