package ai.askdiverge.ui.model.mapper

import ai.askdiverge.LivechatSessionInfo
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.LivechatUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class LivechatSessionInfoMapperTest {

    @Test
    fun `nothing is reported while the session has not been read`() {
        val state = makeSUT(
            LivechatUiState(status = LivechatState.Status.ACTIVE, isStateKnown = false)
        )

        assertNull(state.toLivechatSessionInfo())
    }

    @Test
    fun `an active session reports the agent the header shows`() {
        val state = makeSUT(
            LivechatUiState(
                status = LivechatState.Status.ACTIVE,
                agentDisplayName = "Alice",
                isStateKnown = true
            )
        )

        assertEquals(
            LivechatSessionInfo(LivechatSessionInfo.Status.ACTIVE, agentDisplayName = "Alice"),
            state.toLivechatSessionInfo()
        )
    }

    @Test
    fun `a status this SDK does not know is reported as inactive`() {
        val state = makeSUT(
            LivechatUiState(status = LivechatState.Status.UNKNOWN, isStateKnown = true)
        )

        assertEquals(
            LivechatSessionInfo(LivechatSessionInfo.Status.INACTIVE),
            state.toLivechatSessionInfo()
        )
    }

    @Test
    fun `an expired visitor session is reported as inactive`() {
        val state = makeSUT(
            livechat = LivechatUiState(status = LivechatState.Status.WAITING, isStateKnown = false),
            connectionState = ChatConnectionState.SessionExpired
        )

        assertEquals(
            LivechatSessionInfo(LivechatSessionInfo.Status.INACTIVE),
            state.toLivechatSessionInfo()
        )
    }

    private fun makeSUT(
        livechat: LivechatUiState,
        connectionState: ChatConnectionState = ChatConnectionState.Ready
    ): ChatbotUiState {
        val state = ChatbotUiState(livechat = livechat)
        return state.copy(session = state.session.copy(connectionState = connectionState))
    }
}
