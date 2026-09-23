package ai.askdiverge.data.remote.service

import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.adapter.StreamEventAdapter
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource

internal class ChatbotStreamServiceImpl(
    private val request: Request,
    private val eventSourceFactory: EventSource.Factory,
    private val sendMessageAdapter: JsonAdapter<SendMessageRemote>,
    private val sseAdapter: StreamEventAdapter
) : ChatbotStreamService {

    override fun sendMessage(message: SendMessageRemote): Flow<StreamEventRemote> = callbackFlow {
        val json = sendMessageAdapter.toJson(message)

        val httpRequest = request
            .newBuilder()
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        val eventSource = eventSourceFactory.newEventSource(
            request = httpRequest,
            listener = ChatbotEventSourceListener(
                channel = this,
                sseAdapter = sseAdapter
            )
        )

        awaitClose { eventSource.cancel() }
    }
}
