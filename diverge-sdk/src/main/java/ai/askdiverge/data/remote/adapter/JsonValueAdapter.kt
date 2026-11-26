package ai.askdiverge.data.remote.adapter

import ai.askdiverge.domain.model.metadata.JsonValue
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Writes host session metadata as JSON. A [JsonValue.Null] is written as `null` even though the SDK's
 * Moshi leaves out null members: the host set that key to null. Null members the SDK's own models
 * leave out stay out. The SDK never reads metadata back, so reading is refused.
 */
internal class JsonValueAdapter : JsonAdapter<JsonValue>() {

    override fun toJson(writer: JsonWriter, value: JsonValue?) {
        when (value) {
            null, JsonValue.Null -> writeNull(writer)
            is JsonValue.Str -> writer.value(value.value)
            is JsonValue.Integer -> writer.value(value.value)
            is JsonValue.Decimal -> writer.value(value.value)
            is JsonValue.Bool -> writer.value(value.value)
            is JsonValue.Array -> {
                writer.beginArray()
                value.values.forEach { toJson(writer, it) }
                writer.endArray()
            }
            is JsonValue.Object -> {
                writer.beginObject()
                value.values.forEach { (key, member) ->
                    writer.name(key)
                    toJson(writer, member)
                }
                writer.endObject()
            }
        }
    }

    override fun fromJson(reader: JsonReader): JsonValue =
        throw UnsupportedOperationException("Session metadata is never read")

    // Turned on only for this null, so the member name before it is written too.
    private fun writeNull(writer: JsonWriter) {
        val serializeNulls = writer.serializeNulls
        writer.serializeNulls = true
        try {
            writer.nullValue()
        } finally {
            writer.serializeNulls = serializeNulls
        }
    }
}
