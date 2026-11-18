package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.livechat.LivechatMessageRemote
import ai.askdiverge.data.model.livechat.LivechatSyncMessagesRemote
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Types

private const val SEQUENCE_NUMBER = "sequence_number"

/**
 * Decodes the `messages` array of a sync page through [messagesAdapter], which drops a message
 * that fails to decode, and reads every element's `sequence_number` so the poll cursor moves past
 * a dropped message instead of requesting it again.
 */
internal class LivechatSyncMessagesAdapter(
    private val messagesAdapter: JsonAdapter<List<LivechatMessageRemote>>
) : JsonAdapter<LivechatSyncMessagesRemote>() {

    override fun fromJson(reader: JsonReader): LivechatSyncMessagesRemote {
        val elements = reader.readJsonValue() as? List<*>
            ?: throw JsonDataException("Expected an array of livechat messages at ${reader.path}")
        return LivechatSyncMessagesRemote(
            messages = messagesAdapter.fromJsonValue(elements).orEmpty(),
            lastSequenceNumber = elements
                .mapNotNull { ((it as? Map<*, *>)?.get(SEQUENCE_NUMBER) as? Number)?.toLong() }
                .maxOrNull()
        )
    }

    override fun toJson(
        writer: JsonWriter,
        value: LivechatSyncMessagesRemote?
    ) {
        messagesAdapter.toJson(writer, value?.messages)
    }

    companion object {
        val FACTORY = Factory { type, annotations, moshi ->
            if (type != LivechatSyncMessagesRemote::class.java || annotations.isNotEmpty()) {
                null
            } else {
                LivechatSyncMessagesAdapter(
                    moshi.adapter(Types.newParameterizedType(List::class.java, LivechatMessageRemote::class.java))
                )
            }
        }
    }
}
