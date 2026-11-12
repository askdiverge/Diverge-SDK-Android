package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.domain.exception.ChatbotException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class HistoryLoadRetryTest {

    @Test
    fun `session expiry routes to the session path instead of paging retry`() {
        val actions = mutableListOf<String>()
        onHistoryLoadRetry(
            error = ChatbotException.SessionExpired(),
            onSessionExpired = { actions += "expired" },
            onRetryPaging = { actions += "paging" }
        )
        assertEquals(listOf("expired"), actions)

        actions.clear()
        onHistoryLoadRetry(
            error = ChatbotException.NoTokenAvailable(),
            onSessionExpired = { actions += "expired" },
            onRetryPaging = { actions += "paging" }
        )
        assertEquals(listOf("expired"), actions)
    }

    @Test
    fun `a transient failure retries the failed page`() {
        val actions = mutableListOf<String>()
        onHistoryLoadRetry(
            error = IOException("timeout"),
            onSessionExpired = { actions += "expired" },
            onRetryPaging = { actions += "paging" }
        )
        assertEquals(listOf("paging"), actions)
    }

    @Test
    fun `a null error still retries paging`() {
        val actions = mutableListOf<String>()
        onHistoryLoadRetry(
            error = null,
            onSessionExpired = { actions += "expired" },
            onRetryPaging = { actions += "paging" }
        )
        assertEquals(listOf("paging"), actions)
    }

    @Test
    fun `isHistorySessionExpiry recognises auth failures only`() {
        assertTrue(ChatbotException.SessionExpired().isHistorySessionExpiry())
        assertTrue(ChatbotException.NoTokenAvailable().isHistorySessionExpiry())
        assertFalse(IOException("boom").isHistorySessionExpiry())
    }
}
