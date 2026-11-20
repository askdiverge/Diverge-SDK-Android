package ai.askdiverge.ui.state

import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.ui.livechat.LivechatControl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class LivechatUiStateTest {

    @Test
    fun `in session always shows end`() {
        val state = LivechatUiState(
            config = availableConfig(),
            status = LivechatState.Status.WAITING
        )

        assertEquals(LivechatControl.END, state.control)
    }

    @Test
    fun `enabled live logo while live shows start`() {
        val state = LivechatUiState(config = availableConfig())

        assertEquals(LivechatControl.START, state.control)
    }

    @Test
    fun `enabled live logo while offline shows offline`() {
        val state = LivechatUiState(
            config = availableConfig().copy(
                availabilityStatus = LivechatConfig.AvailabilityStatus.OFFLINE
            )
        )

        assertEquals(LivechatControl.OFFLINE, state.control)
    }

    @Test
    fun `disabled or logo hidden hides the control`() {
        assertEquals(
            LivechatControl.HIDDEN,
            LivechatUiState(config = availableConfig().copy(enabled = false)).control
        )
        assertEquals(
            LivechatControl.HIDDEN,
            LivechatUiState(config = availableConfig().copy(showLivechatLogo = false)).control
        )
    }

    @Test
    fun `active agent name overrides the header title`() {
        val state = LivechatUiState(
            status = LivechatState.Status.ACTIVE,
            agentDisplayName = "Alex"
        )

        assertEquals("Alex", state.headerTitleOverride)
        assertNull(
            LivechatUiState(
                status = LivechatState.Status.WAITING,
                agentDisplayName = "Alex"
            ).headerTitleOverride
        )
    }

    private fun availableConfig() = LivechatConfig.serverDefaults.copy(
        enabled = true,
        configured = true,
        availabilityStatus = LivechatConfig.AvailabilityStatus.LIVE,
        showLivechatLogo = true
    )
}
