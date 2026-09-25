package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.event.EventTypeRemote
import ai.askdiverge.data.model.event.StreamDoneEventRemote
import ai.askdiverge.data.model.event.StreamErrorEventRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.event.StreamPartDeltaEventRemote
import ai.askdiverge.data.model.event.StreamPartEventRemote
import ai.askdiverge.data.model.event.StreamStatusEventRemote
import com.squareup.moshi.Moshi
import java.io.IOException
import okhttp3.sse.EventSource

internal abstract class SseAdapter<T> {

    @Throws(IOException::class)
    abstract fun fromStreamEvent(
        eventSource: EventSource,
        id: String?,
        type: String?,
        data: String
    ): T
}

internal class StreamEventAdapter(private val moshi: Moshi) : SseAdapter<StreamEventRemote?>() {

    private val statusAdapter = moshi.adapter(StreamStatusEventRemote::class.java)
    private val partEventAdapter = moshi.adapter(StreamPartEventRemote::class.java)
    private val partDeltaAdapter = moshi.adapter(StreamPartDeltaEventRemote::class.java)
    private val doneEventAdapter = moshi.adapter(StreamDoneEventRemote::class.java)
    private val errorEventAdapter = moshi.adapter(StreamErrorEventRemote::class.java)

    @Throws(IOException::class)
    override fun fromStreamEvent(
        eventSource: EventSource,
        id: String?,
        type: String?,
        data: String
    ): StreamEventRemote? {
        val eventType = EventTypeRemote.fromType(type) ?: return null

        val content = when (eventType) {
            EventTypeRemote.STATUS -> statusAdapter.fromJson(data)
            EventTypeRemote.PART -> partEventAdapter.fromJson(data)
            EventTypeRemote.PART_DELTA -> partDeltaAdapter.fromJson(data)
            EventTypeRemote.DONE -> doneEventAdapter.fromJson(data)
            EventTypeRemote.ERROR -> errorEventAdapter.fromJson(data)
        }

        if (content == null) {
            return null
        }

        return content
    }
}
