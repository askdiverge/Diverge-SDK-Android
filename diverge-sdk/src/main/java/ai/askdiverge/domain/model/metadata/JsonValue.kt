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
 * The deepest level the backend takes a value at; the top-level map is level 0. It also stops a map
 * or list that contains itself.
 */
private const val MAX_DEPTH = 20

/** Keys the backend refuses at any depth. */
private val BLOCKED_KEYS = setOf("__proto__", "prototype", "constructor")

/** Top-level keys the backend keeps for verified metadata. */
private val RESERVED_KEYS = setOf("verified_metadata", "signed_metadata")

/**
 * These host values as JSON, or `null` when the backend would refuse them as a patch, its 32 KiB
 * limit aside: a reserved top-level key, or at any depth a value that is not a `String`, `Boolean`,
 * `Int`, `Long`, finite `Float` or `Double`, `null`, `List` or `Map` with `String` keys, a value
 * nested deeper than [MAX_DEPTH], or a blocked key.
 */
internal fun Map<*, *>.toJsonValuesOrNull(): Map<String, JsonValue>? {
    if (keys.any { it in RESERVED_KEYS }) return null
    return (toJsonValueOrNull(depth = 0) as? JsonValue.Object)?.values
}

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
                if (key !is String || key in BLOCKED_KEYS) return null
                key to (value.toJsonValueOrNull(depth + 1) ?: return null)
            }
        )
        else -> null
    }
}
