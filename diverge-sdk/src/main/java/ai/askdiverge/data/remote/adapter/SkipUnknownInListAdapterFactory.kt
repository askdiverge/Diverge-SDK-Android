package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBlockRemote
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A Moshi [JsonAdapter.Factory] that removes unknown items from lists of [MessagePartRemote]
 * and [RichTextBlockRemote].
 *
 * The polymorphic adapters for these sealed types are configured with `.withDefaultValue(null)`
 * (see [NetworkAdapter]), meaning any subtype the app doesn't recognize is deserialized as `null`.
 * This factory intercepts those lists and strips out the nulls so the rest of the app only
 * sees supported types.
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

        return SkipNullsListAdapter(delegate)
    }

    private class SkipNullsListAdapter(private val delegate: JsonAdapter<List<Any?>>) : JsonAdapter<List<Any?>>() {

        override fun fromJson(reader: JsonReader): List<Any?>? = delegate.fromJson(reader)?.filterNotNull()

        override fun toJson(
            writer: JsonWriter,
            value: List<Any?>?
        ) {
            delegate.toJson(writer, value)
        }
    }
}
