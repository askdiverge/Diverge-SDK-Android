package ai.askdiverge.data.remote.service

import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.adapter.StreamEventAdapter
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource

internal class ChatbotStreamServiceImpl(
    private val request: Request,
    private val eventSourceFactory: EventSource.Factory,
    private val sendMessageAdapter: JsonAdapter<SendMessageRemote>,
    private val sseAdapter: StreamEventAdapter,
    private val ioDispatcher: CoroutineDispatcher
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
        // The body can carry several photos, so it is serialized off the collector's thread.
        .flowOn(ioDispatcher)
}
