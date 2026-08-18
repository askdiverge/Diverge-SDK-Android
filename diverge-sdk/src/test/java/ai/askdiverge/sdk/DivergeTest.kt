package ai.askdiverge.sdk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DivergeTest {
    @After
    fun tearDown() {
        Diverge.reset()
    }

    @Test
    fun versionMatchesBuildConfig() {
        assertEquals(BuildConfig.SDK_VERSION, Diverge.VERSION)
    }

    @Test
    fun versionIsSemVerCore() {
        assertTrue(
            Diverge.VERSION.matches(Regex("""^\d+\.\d+\.\d+([.-][\w.-]+)?(\+[\w.-]+)?$""")),
        )
        assertNotEquals("0.0.0", Diverge.VERSION)
        assertTrue(Diverge.VERSION.isNotBlank())
    }

    @Test
    fun configureRequiresNonEmptyApiKey() {
        try {
            Diverge.configure(DivergeConfiguration(apiKey = "  ", environment = DivergeEnvironment.SANDBOX))
            fail("Expected InvalidApiKey")
        } catch (error: DivergeException.InvalidApiKey) {
            assertEquals("API key must not be blank.", error.message)
            assertFalse(Diverge.isConfigured)
        }
    }

    @Test
    fun configureSandboxClient() {
        val client = Diverge.configure(
            DivergeConfiguration(apiKey = "sk_test_123", environment = DivergeEnvironment.SANDBOX),
        )
        assertTrue(Diverge.isConfigured)
        assertEquals(DivergeEnvironment.SANDBOX, client.configuration.environment)
        assertEquals("sandbox", client.configuration.environment.wireName)
        assertEquals("https://sandbox.api.askdiverge.ai", client.apiBaseUrl)
        assertEquals("sk_test_123", Diverge.shared.configuration.apiKey)
    }

    @Test
    fun configureProductionClient() {
        val client = Diverge.configure(
            DivergeConfiguration(apiKey = "sk_live_123", environment = DivergeEnvironment.PRODUCTION),
        )
        assertEquals(DivergeEnvironment.PRODUCTION, client.configuration.environment)
        assertEquals("production", client.configuration.environment.wireName)
        assertEquals("https://api.askdiverge.ai", client.apiBaseUrl)
    }

    @Test
    fun sharedThrowsWhenNotConfigured() {
        try {
            Diverge.shared
            fail("Expected NotConfigured")
        } catch (_: DivergeException.NotConfigured) {
            // expected
        }
    }

    @Test
    fun configurationToStringRedactsApiKey() {
        val configuration = DivergeConfiguration(
            apiKey = "sk_sandbox_secret_value",
            environment = DivergeEnvironment.SANDBOX,
        )
        val text = configuration.toString()
        assertFalse(text.contains("secret_value"))
        assertTrue(text.contains("…"))
        assertTrue(text.contains("SANDBOX"))
    }

    @Test
    fun concurrentConfigureAndShared() {
        val iterations = 50
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(iterations)
        val failures = AtomicInteger(0)

        repeat(iterations) { index ->
            pool.execute {
                try {
                    Diverge.configure(
                        DivergeConfiguration(
                            apiKey = "sk_concurrent_$index",
                            environment = DivergeEnvironment.SANDBOX,
                        ),
                    )
                    Diverge.shared
                } catch (_: Exception) {
                    failures.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        pool.shutdown()
        assertEquals(0, failures.get())
        assertTrue(Diverge.isConfigured)
    }

    @Test
    fun statusViewDumpIsStable() {
        val notConfigured = DivergeStatusView.accessibilityDump(null)
        assertTrue(notConfigured.contains("title: Diverge SDK"))
        assertTrue(notConfigured.contains("not-configured"))
        assertTrue(notConfigured.contains(Diverge.VERSION))
        assertEquals(3, notConfigured.lines().size)

        val client = Diverge.configure(
            DivergeConfiguration(apiKey = "sk_dump", environment = DivergeEnvironment.SANDBOX),
        )
        val configured = DivergeStatusView.accessibilityDump(client)
        assertTrue(configured.contains("environment: sandbox"))
        assertTrue(configured.contains("apiBaseURL: https://sandbox.api.askdiverge.ai"))
        assertFalse(configured.contains("sk_dump"))
        assertEquals(4, configured.lines().size)
    }

    @Test
    fun statusViewDumpMatchesIosContractKeys() {
        val keys = listOf("title:", "version:", "state:")
        val dump = DivergeStatusView.accessibilityDump(null)
        keys.forEach { key ->
            assertTrue("missing $key in dump", dump.contains(key))
        }
    }
}
