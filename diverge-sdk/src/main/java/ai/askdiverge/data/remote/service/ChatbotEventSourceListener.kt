package ai.askdiverge.data.remote.service

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.remote.adapter.StreamEventAdapter
import android.util.Log
import java.net.HttpURLConnection
import kotlinx.coroutines.channels.SendChannel
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okio.IOException

internal class ChatbotEventSourceListener(
    private val channel: SendChannel<StreamEventRemote>,
    private val sseAdapter: StreamEventAdapter
) : EventSourceListener() {

    override fun onEvent(
        eventSource: EventSource,
        id: String?,
        type: String?,
        data: String
    ) {
        val event = try {
            sseAdapter.fromStreamEvent(
                eventSource = eventSource,
                id = id,
                type = type,
                data = data
            )
        } catch (ex: Exception) {
            Log.e("Diverge", ex.message ?: "")
            return
        }
        if (event == null) return
        channel.trySend(event)
    }

    override fun onFailure(
        eventSource: EventSource,
        t: Throwable?,
        response: Response?
    ) {
        val error = if (response?.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            ChatbotException.SessionExpired()
        } else {
            t ?: IOException("SSE connection failed")
        }
        channel.close(error)
    }

    override fun onClosed(eventSource: EventSource) {
        channel.close()
    }
}
