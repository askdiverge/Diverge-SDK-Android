package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBlockRemote
import ai.askdiverge.domain.logging.ChatbotLogger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A Moshi [JsonAdapter.Factory] that removes unknown and malformed items from lists of
 * [MessagePartRemote] and [RichTextBlockRemote].
 *
 * The polymorphic adapters for these sealed types fall back to [UnknownTypeLoggingAdapter]
 * (see [NetworkAdapter]), meaning any subtype the app doesn't recognize is deserialized as `null`.
 * An item of a known type that fails to decode (a required field missing or of the wrong type) is
 * logged and dropped. The rest of the list, and the message that holds it, still decode.
 */
internal class SkipUnknownInListAdapterFactory(private val targetTypes: Set<Class<*>>) : JsonAdapter.Factory {

    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        if (annotations.isNotEmpty()) return null

        val rawType = Types.getRawType(type)
        if (rawType != List::class.java) return null
        if (type !is ParameterizedType) return null

        val elementType = Types.collectionElementType(type, List::class.java)
        if (Types.getRawType(elementType) !in targetTypes) return null

        val delegate = moshi.nextAdapter<List<Any?>>(this, type, annotations)
        val elementAdapter = moshi.adapter<Any?>(elementType)

        return SkipNullsListAdapter(delegate, elementAdapter)
    }

    private class SkipNullsListAdapter(
        private val delegate: JsonAdapter<List<Any?>>,
        private val elementAdapter: JsonAdapter<Any?>
    ) : JsonAdapter<List<Any?>>() {

        override fun fromJson(reader: JsonReader): List<Any?>? {
            if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
            val items = mutableListOf<Any?>()
            reader.beginArray()
            while (reader.hasNext()) {
                // Decode from a peeked copy so a failed item can still be skipped as a whole.
                val item = try {
                    elementAdapter.fromJson(reader.peekJson())
                } catch (error: JsonDataException) {
                    ChatbotLogger.logError(error)
                    null
                }
                reader.skipValue()
                if (item != null) items += item
            }
            reader.endArray()
            return items
        }

        override fun toJson(
            writer: JsonWriter,
            value: List<Any?>?
        ) {
            delegate.toJson(writer, value)
        }
    }
}
