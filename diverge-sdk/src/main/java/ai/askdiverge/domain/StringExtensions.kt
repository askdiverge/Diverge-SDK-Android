package ai.askdiverge.domain

/** This string without surrounding whitespace, or null when that leaves nothing. */
internal fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
