package ai.askdiverge.di

import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.DivergeEnvironment
import androidx.annotation.RestrictTo
import ai.askdiverge.data.ChatbotRepositoryImpl
import ai.askdiverge.data.interceptor.BearerTokenInterceptor
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.interceptor.SdkHeadersInterceptor
import ai.askdiverge.data.local.datasource.TokenLocalDataSource
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.adapter.StreamEventAdapter
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSourceImpl
import ai.askdiverge.data.remote.service.ChatbotService
import ai.askdiverge.data.remote.service.ChatbotStreamServiceImpl
import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.usecase.DeleteChatDataUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageIdUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.sdk.BuildConfig
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Sets up the chatbot's dependency graph: one HTTP stack and one repository, both internal, handed
 * out only as the use cases the chatbot UI needs. Public so chatbot-ui can build it, restricted so
 * app-level callers cannot depend on it directly.
 *
 * One is built per ChatbotViewModel and dies with it, so the token it caches lives exactly as long
 * as the screen's ViewModel.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal class ChatbotDependencies(
    chatbotCallbacks: ChatbotCallbacks,
    environment: DivergeEnvironment
) {

    private val repository: ChatbotRepository =
        createRepository(chatbotCallbacks, environment)
    val initializeUseCase: InitializeUseCase = InitializeUseCase(repository)
    val startSessionUseCase: StartSessionUseCase = StartSessionUseCase(repository)
    val invalidateSessionUseCase: InvalidateSessionUseCase = InvalidateSessionUseCase(repository)
    val getSessionInvalidationsUseCase: GetSessionInvalidationsUseCase =
        GetSessionInvalidationsUseCase(repository)
    val sendMessageUseCase: SendMessageUseCase = SendMessageUseCase(repository)
    val getMessageHistoryUseCase: GetMessageHistoryUseCase = GetMessageHistoryUseCase(repository)
    val getLatestMessageIdUseCase: GetLatestMessageIdUseCase = GetLatestMessageIdUseCase(repository)
    val renewTokenUseCase: RenewTokenUseCase = RenewTokenUseCase(repository)
    val deleteChatDataUseCase: DeleteChatDataUseCase = DeleteChatDataUseCase(repository)
}

private fun createRepository(
    chatbotCallbacks: ChatbotCallbacks,
    environment: DivergeEnvironment
): ChatbotRepository {
    val tokenLocalDataSource = TokenLocalDataSourceImpl()

    return ChatbotRepositoryImpl(
        chatbotCallbacks = chatbotCallbacks,
        tokenLocalDataSource = tokenLocalDataSource,
        remoteDataSource = createRemoteDataSource(
            environment = environment,
            okHttp = buildOkHttpClient(
                tokenLocalDataSource = tokenLocalDataSource,
                sdkVersion = BuildConfig.SDK_VERSION,
                clientProfiles = listOf(ClientProfile.PRODUCT_RECOMMENDATION)
            )
        )
    )
}

/**
 * The Chatbot API of [environment], every call sent through [okHttp]. The client is passed in, not
 * built here, so what leaves the SDK can be observed at the point it leaves.
 */
internal fun createRemoteDataSource(
    environment: DivergeEnvironment,
    okHttp: OkHttpClient
): ChatbotRemoteDataSource {
    val sseOkHttp = okHttp.newBuilder()
        .readTimeout(
            timeout = 5,
            unit = TimeUnit.MINUTES
        )
        .build()
    val moshi = buildMoshi()

    return ChatbotRemoteDataSourceImpl(
        service = Retrofit.Builder()
            .baseUrl(environment.baseUrl)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChatbotService::class.java),
        streamService = ChatbotStreamServiceImpl(
            request = Request.Builder().url("${environment.baseUrl}v1/chat/messages").build(),
            eventSourceFactory = EventSources.createFactory(sseOkHttp),
            sendMessageAdapter = buildSendMessageAdapter(moshi),
            sseAdapter = buildSseAdapter(moshi)
        )
    )
}

private fun buildMoshi(): Moshi = Moshi.Builder()
    .buildConversationModels()
    .build()

private fun buildSendMessageAdapter(moshi: Moshi) = moshi.adapter(SendMessageRemote::class.java)

private fun buildSseAdapter(moshi: Moshi) = StreamEventAdapter(moshi)

/**
 * The client for authenticated Chatbot API calls. Each request carries the visitor's bearer token
 * and identifies the SDK release and the features it requests (see [SdkHeadersInterceptor]).
 *
 * @param sdkVersion SemVer of the SDK release, from `VERSION`.
 * @param clientProfiles every feature this build requests.
 */
internal fun buildOkHttpClient(
    tokenLocalDataSource: TokenLocalDataSource,
    sdkVersion: String,
    clientProfiles: List<ClientProfile>
): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(BearerTokenInterceptor(tokenLocalDataSource))
    .addInterceptor(SdkHeadersInterceptor(sdkVersion, clientProfiles))
    .connectTimeout(
        timeout = 1,
        unit = TimeUnit.MINUTES
    )
    .readTimeout(
        timeout = 1,
        unit = TimeUnit.MINUTES
    )
    .writeTimeout(
        timeout = 1,
        unit = TimeUnit.MINUTES
    )
    .build()
