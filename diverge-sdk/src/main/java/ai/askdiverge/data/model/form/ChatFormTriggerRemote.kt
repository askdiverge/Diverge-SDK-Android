package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The moment in the conversation the form belongs to, chosen by the operator who designs the form.
 * The backend returns it with the form and, for `llm`, gives the assistant the form's prompt hint.
 * An entry is added when the backend introduces a new moment. Unrecognised wire values decode as
 * [UNKNOWN].
 */
@Keep
@JsonClass(generateAdapter = false)
internal enum class ChatFormTriggerRemote {
    @Json(name = "session_start")
    SESSION_START,

    @Json(name = "livechat_waiting")
    LIVECHAT_WAITING,

    @Json(name = "llm")
    LLM,

    UNKNOWN
}
