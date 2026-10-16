package ai.askdiverge.data.remote.adapter

import ai.askdiverge.domain.logging.ChatbotLogger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Fallback for a polymorphic type: logs the unrecognised [labelKey] value and reads the object as `null`.
 */
internal class UnknownTypeLoggingAdapter(
    private val name: String,
    private val labelKey: String
) : JsonAdapter<Any>() {

    override fun fromJson(reader: JsonReader): Any? {
        val label = (reader.peekJson().readJsonValue() as? Map<*, *>)?.get(labelKey)
        ChatbotLogger.logError("Unknown $name: $label")
        reader.skipValue()
        return null
    }

    override fun toJson(
        writer: JsonWriter,
        value: Any?
    ): Unit = throw IllegalArgumentException("Unknown chatbot $name cannot be written: $value")
}
