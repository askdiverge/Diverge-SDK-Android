package ai.askdiverge.data.mapper

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import retrofit2.Response

/** Maps a 401 on a visitor endpoint to [ChatbotException.SessionExpired]. */
internal fun Response<*>.mapAuthError(): Throwable? =
    if (code() == 401) ChatbotException.SessionExpired() else null

/**
 * Livechat visitor endpoints: 401 ends the visitor session; 409 means unavailable or nothing open.
 */
internal fun Response<*>.mapLivechatError(): Throwable? = when (code()) {
    401 -> ChatbotException.SessionExpired()
    409 -> LivechatRequestException.Conflict()
    else -> null
}
