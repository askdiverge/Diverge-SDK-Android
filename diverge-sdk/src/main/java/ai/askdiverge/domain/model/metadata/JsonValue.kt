package ai.askdiverge.domain.model.metadata

/**
 * A JSON value the host set as session metadata, converted at the call so no host object reaches
 * the encoder.
 */
internal sealed interface JsonValue {

    /** A JSON string. */
    data class Str(val value: String) : JsonValue

    /** A JSON number without a fraction, from an `Int` or a `Long`. */
    data class Integer(val value: Long) : JsonValue

    /** A finite JSON number with a fraction, from a `Float` or a `Double`. */
    data class Decimal(val value: Double) : JsonValue

    /** A JSON `true` or `false`. */
    data class Bool(val value: Boolean) : JsonValue

    /** A JSON array; the backend replaces an earlier array with it as a whole. */
    data class Array(val values: List<JsonValue>) : JsonValue

    /** A JSON object; the backend merges it into an earlier object under the same key. */
    data class Object(val values: Map<String, JsonValue>) : JsonValue

    /** A JSON `null`; it replaces an earlier value, and the key stays. */
    data object Null : JsonValue
}

/**
 * Deepest nesting a host value may have. It stops a map or list that contains itself; the backend
 * applies its own, lower limit.
 */
private const val MAX_DEPTH = 64

/**
 * These host values as JSON, or `null` when one of them, at any depth, is not a `String`,
 * `Boolean`, `Int`, `Long`, finite `Float` or `Double`, `null`, `List` or `Map` with `String` keys,
 * or is nested deeper than [MAX_DEPTH].
 */
internal fun Map<*, *>.toJsonValuesOrNull(): Map<String, JsonValue>? =
    (toJsonValueOrNull(depth = 0) as? JsonValue.Object)?.values

private fun Any?.toJsonValueOrNull(depth: Int): JsonValue? {
    if (depth > MAX_DEPTH) return null
    return when (this) {
        null -> JsonValue.Null
        is String -> JsonValue.Str(this)
        is Boolean -> JsonValue.Bool(this)
        is Int -> JsonValue.Integer(toLong())
        is Long -> JsonValue.Integer(this)
        // Through its shortest decimal form, so 0.1f goes out as 0.1.
        is Float -> if (isFinite()) JsonValue.Decimal(toString().toDouble()) else null
        is Double -> if (isFinite()) JsonValue.Decimal(this) else null
        is List<*> -> JsonValue.Array(map { it.toJsonValueOrNull(depth + 1) ?: return null })
        is Map<*, *> -> JsonValue.Object(
            entries.associate { (key, value) ->
                (key as? String ?: return null) to (value.toJsonValueOrNull(depth + 1) ?: return null)
            }
        )
        else -> null
    }
}
