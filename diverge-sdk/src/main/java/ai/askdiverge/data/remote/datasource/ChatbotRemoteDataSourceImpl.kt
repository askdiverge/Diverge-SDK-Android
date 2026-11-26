package ai.askdiverge.data.remote.datasource

import ai.askdiverge.data.mapper.mapAuthError
import ai.askdiverge.data.mapper.mapFormError
import ai.askdiverge.data.mapper.mapLivechatError
import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.banner.ChatBannerListRemote
import ai.askdiverge.data.model.error.ApiErrorEnvelopeRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.ChatFormValuesPatchRequestRemote
import ai.askdiverge.data.model.form.ChatSessionStateRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatCloseRequestRemote
import ai.askdiverge.data.model.livechat.LivechatFeedbackRequestRemote
import ai.askdiverge.data.model.livechat.LivechatHandoverRequestRemote
import ai.askdiverge.data.model.livechat.LivechatSessionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatSyncRemote
import ai.askdiverge.data.model.livechat.LivechatTypingRequestRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageRequestRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageResponseRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.model.rating.ConversationRatingRequestRemote
import ai.askdiverge.data.model.session.ChatSessionPatchRequestRemote
import ai.askdiverge.data.remote.service.ChatbotService
import ai.askdiverge.data.remote.service.ChatbotStreamService
import ai.askdiverge.data.runCatchingCancellable
import ai.askdiverge.data.safeApiCall
import ai.askdiverge.domain.exception.SessionMetadataRequestException
import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.model.export.ExportDestination
import com.squareup.moshi.JsonAdapter
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.sink
import retrofit2.Response
import retrofit2.awaitResponse

internal class ChatbotRemoteDataSourceImpl(
    private val service: ChatbotService,
    private val streamService: ChatbotStreamService,
    private val errorEnvelopeAdapter: JsonAdapter<ApiErrorEnvelopeRemote>
) : ChatbotRemoteDataSource {

    override suspend fun getConfig(): Result<ConfigRemote> = safeApiCall(
        errorMessage = "Config error",
        mapError = { it.mapAuthError() }
    ) {
        service.config()
    }

    override suspend fun getMessageHistory(
        cursor: String?,
        pageSize: Int
    ): Result<MessagePageRemote> = safeApiCall(
        errorMessage = "Could not get messages",
        mapError = { it.mapAuthError() }
    ) {
        service.getMessageHistory(
            cursor = cursor,
            limit = pageSize
        )
    }

    override fun sendMessage(message: SendMessageRemote): Flow<StreamEventRemote> =
        streamService.sendMessage(message)

    override suspend fun submitAction(body: SubmitActionRequestRemote): Result<SubmitActionResponseRemote> =
        safeApiCall(
            errorMessage = "Could not submit action",
            mapError = { it.mapFormError(errorEnvelopeAdapter) }
        ) {
            service.submitAction(body)
        }

    override suspend fun fetchForm(formId: String): Result<ChatFormDefinitionRemote> =
        safeApiCall(
            errorMessage = "Could not fetch form",
            mapError = { it.mapFormError(errorEnvelopeAdapter) }
        ) {
            service.fetchForm(formId)
        }

    override suspend fun getSession(): Result<ChatSessionStateRemote> =
        safeApiCall(
            errorMessage = "Could not get session",
            mapError = { it.mapFormError(errorEnvelopeAdapter) }
        ) {
            service.getSession()
        }

    override suspend fun patchFormValues(
        formId: String,
        body: ChatFormValuesPatchRequestRemote
    ): Result<ChatSessionStateRemote> =
        safeApiCall(
            errorMessage = "Could not patch form values",
            mapError = { it.mapFormError(errorEnvelopeAdapter) }
        ) {
            service.patchFormValues(formId, body)
        }

    // The status goes to the caller as it came: a 401 here must neither end the chat nor be taken
    // for a connection failure, as a mapped SessionExpired, an IOException, would be.
    override suspend fun patchSession(body: ChatSessionPatchRequestRemote): Result<Unit> =
        emptyBodyCall(
            errorMessage = "Could not patch session metadata",
            mapError = { SessionMetadataRequestException(status = it.code()) }
        ) {
            service.patchSession(body)
        }

    override suspend fun syncLivechat(
        afterSequenceNumber: Long?,
        waitMs: Int?,
        syncCursor: String?
    ): Result<LivechatSyncRemote> = safeApiCall(
        errorMessage = "Could not sync livechat",
        mapError = { it.mapAuthError() }
    ) {
        service.syncLivechat(
            afterSequenceNumber = afterSequenceNumber,
            waitMs = waitMs,
            syncCursor = syncCursor
        )
    }

    override suspend fun requestLivechatHandover(
        body: LivechatHandoverRequestRemote
    ): Result<LivechatSessionResponseRemote> = safeApiCall(
        errorMessage = "Could not request livechat handover",
        mapError = { it.mapLivechatError() }
    ) {
        service.requestLivechatHandover(body)
    }

    override suspend fun sendLivechatMessage(
        body: LivechatVisitorMessageRequestRemote
    ): Result<LivechatVisitorMessageResponseRemote> = safeApiCall(
        errorMessage = "Could not send livechat message",
        mapError = { it.mapAuthError() }
    ) {
        // Retrofit serializes the body on the calling thread, and it can carry several photos.
        withContext(Dispatchers.IO) { service.sendLivechatMessage(body) }
    }

    override suspend fun closeLivechat(body: LivechatCloseRequestRemote): Result<Unit> =
        emptyBodyCall(
            errorMessage = "Could not close livechat",
            mapError = { it.mapLivechatError() }
        ) {
            service.closeLivechat(body)
        }

    // Not logged here: typing posts repeat while the visitor types, so the caller logs once.
    override suspend fun sendLivechatTyping(body: LivechatTypingRequestRemote): Result<Unit> =
        emptyBodyCall(
            errorMessage = "Could not send livechat typing",
            mapError = { it.mapLivechatError() },
            logFailure = false
        ) {
            service.sendLivechatTyping(body)
        }

    override suspend fun submitLivechatFeedback(body: LivechatFeedbackRequestRemote): Result<Unit> =
        emptyBodyCall(
            errorMessage = "Could not submit livechat feedback",
            mapError = { it.mapLivechatError() }
        ) {
            service.submitLivechatFeedback(body)
        }

    override suspend fun rateConversation(body: ConversationRatingRequestRemote): Result<Unit> =
        emptyBodyCall(
            errorMessage = "Could not submit conversation rating",
            mapError = { it.mapAuthError() }
        ) {
            service.rateConversation(body)
        }

    override suspend fun exportMyData(destination: ExportDestination): Result<Unit> {
        var saved = false
        try {
            val call = service.exportMyData()
            return safeApiCall(
                errorMessage = EXPORT_ERROR_MESSAGE,
                mapError = { it.mapAuthError() }
            ) {
                call.awaitResponse()
            }.fold(
                onSuccess = { body -> writeJsonObject(body, destination, abort = call::cancel) },
                onFailure = { error -> Result.failure(error) }
            ).also { result -> saved = result.isSuccess }
        } finally {
            if (!saved) deleteDestination(destination)
        }
    }

    override suspend fun discardExport(destination: ExportDestination): Result<Unit> =
        deleteDestination(destination)

    override suspend fun listBanners(url: String?): Result<ChatBannerListRemote> = safeApiCall(
        errorMessage = "Could not load banners",
        mapError = { it.mapAuthError() }
    ) {
        service.listBanners(url = url)
    }

    // The body is copied to the visitor's document as it arrives, so the export is never held in
    // memory whole. The document is opened only once the body is known to be a JSON object.
    // Cancelling calls [abort], which ends a read that is waiting on the network.
    private suspend fun writeJsonObject(
        body: ResponseBody,
        destination: ExportDestination,
        abort: () -> Unit
    ): Result<Unit> = runCatchingCancellable<Unit> {
        try {
            withContext(Dispatchers.IO) {
                val copy = async {
                    try {
                        val source = body.source()
                        if (!source.startsWithJsonObject()) throw Exception(EXPORT_ERROR_MESSAGE)
                        destination.openOutputStream().sink().buffer().use { sink ->
                            // A segment at a time, so a cancelled export stops at the next one.
                            while (source.read(sink.buffer, COPY_SEGMENT_BYTES) != -1L) {
                                ensureActive()
                                sink.emitCompleteSegments()
                            }
                        }
                    } catch (error: IOException) {
                        // The read an abort ended reports the cancellation that caused it.
                        ensureActive()
                        throw error
                    }
                }
                try {
                    copy.await()
                } catch (cancellation: CancellationException) {
                    abort()
                    throw cancellation
                }
            }
        } finally {
            // Closing a partly read body can touch the socket, so it runs off the main thread.
            withContext(NonCancellable + Dispatchers.IO) { body.close() }
        }
    }.onFailure { error -> ChatbotLogger.logError(error) }

    // Non-cancellable, so a cancelled export still removes its document.
    private suspend fun deleteDestination(destination: ExportDestination): Result<Unit> =
        withContext(NonCancellable + Dispatchers.IO) {
            runCatchingCancellable { destination.delete() }
                .onFailure { error -> ChatbotLogger.logError(error) }
        }

    private suspend fun emptyBodyCall(
        errorMessage: String,
        mapError: (Response<*>) -> Throwable? = { it.mapAuthError() },
        logFailure: Boolean = true,
        call: suspend () -> Response<ResponseBody>
    ): Result<Unit> = runCatchingCancellable {
        val response = call()
        if (response.isSuccessful) {
            Unit
        } else {
            throw mapError(response) ?: Exception(errorMessage)
        }
    }.onFailure { error -> if (logFailure) ChatbotLogger.logError(error) }
}

private const val EXPORT_ERROR_MESSAGE = "Could not export data"

/** Bytes the export copies per read: one okio segment. */
private const val COPY_SEGMENT_BYTES = 8_192L

/**
 * True when the body begins with `{` after ASCII whitespace — a JSON object, not an array. Reads
 * through [BufferedSource.peek], so nothing is consumed.
 */
private fun BufferedSource.startsWithJsonObject(): Boolean {
    val peek = peek()
    while (peek.request(1)) {
        val byte = peek.readByte().toInt()
        if (byte != ' '.code && byte != '\t'.code && byte != '\n'.code && byte != '\r'.code) {
            return byte == '{'.code
        }
    }
    return false
}
