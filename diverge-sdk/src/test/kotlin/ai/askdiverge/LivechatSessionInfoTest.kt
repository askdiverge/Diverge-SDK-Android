package ai.askdiverge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class LivechatSessionInfoTest {

    @Test
    fun `the agent name is kept only while active and a blank one is dropped`() {
        assertEquals(
            "Alice",
            LivechatSessionInfo.of(
                status = LivechatSessionInfo.Status.ACTIVE,
                agentName = "  Alice "
            ).agentDisplayName
        )
        assertNull(
            LivechatSessionInfo.of(
                status = LivechatSessionInfo.Status.ACTIVE,
                agentName = "   "
            ).agentDisplayName
        )
        assertNull(
            LivechatSessionInfo.of(
                status = LivechatSessionInfo.Status.CLOSED,
                agentName = "Alice"
            ).agentDisplayName
        )
        assertNull(
            LivechatSessionInfo.of(
                status = LivechatSessionInfo.Status.WAITING,
                agentName = "Alice"
            ).agentDisplayName
        )
    }
}
