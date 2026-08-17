package ai.askdiverge.sdk

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * Pixel snapshots. Goldens live under `src/test/snapshots`.
 * Record with `make android-paparazzi-record`. CI fails if goldens drift or are missing.
 */
class DivergeStatusViewPaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @After
    fun tearDown() {
        Diverge.reset()
    }

    @Test
    fun statusViewNotConfigured() {
        val view = DivergeStatusView(paparazzi.context)
        view.bind(null)
        paparazzi.snapshot(view)
    }

    @Test
    fun statusViewConfiguredSandbox() {
        val client = Diverge.configure(
            Configuration(apiKey = "sk_test_a11y", environment = Environment.SANDBOX),
        )
        val view = DivergeStatusView(paparazzi.context)
        view.bind(client)
        paparazzi.snapshot(view)
    }
}
