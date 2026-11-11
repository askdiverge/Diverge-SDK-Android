package ai.askdiverge.data.mapper

import ai.askdiverge.domain.exception.ChatbotException
import retrofit2.Response

/** Maps a 401 on a visitor endpoint to [ChatbotException.SessionExpired]. */
internal fun Response<*>.mapAuthError(): Throwable? =
    if (code() == 401) ChatbotException.SessionExpired() else null
