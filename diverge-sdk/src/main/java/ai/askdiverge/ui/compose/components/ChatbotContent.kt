package ai.askdiverge.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import ai.askdiverge.ui.compose.components.bottomsheet.ChatDataDeletionBottomSheet
import ai.askdiverge.ui.compose.components.bottomsheet.PrivacyPolicyBottomSheet
import ai.askdiverge.ui.compose.components.conversation.ConversationList
import ai.askdiverge.ui.compose.components.conversation.ScrollToBottomButton
import ai.askdiverge.ui.compose.extension.measureInputBarHeight
import ai.askdiverge.ui.compose.previewprovider.ChatbotUiStatePreviewProvider
import ai.askdiverge.ui.compose.previewprovider.emptyEndOfPaginationHistory
import ai.askdiverge.ui.compose.theme.ChatbotTheme
import ai.askdiverge.ui.compose.theme.screenBackground
import ai.askdiverge.ui.form.FormPhotoDestination
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.state.SnackbarType
import ai.askdiverge.sdk.R
import ai.askdiverge.domain.model.form.ConversationForm
import kotlinx.coroutines.launch
import java.io.InputStream

/** Saves a [FormPhotoDestination] as `[partId, key]` for a field or `[partId]` for a ticket. */
private val PhotoDestinationSaver = Saver<FormPhotoDestination?, ArrayList<String>>(
    save = { destination ->
        when (destination) {
            is FormPhotoDestination.FormField -> arrayListOf(destination.partId, destination.key)
            is FormPhotoDestination.TicketAttachment -> arrayListOf(destination.partId)
            null -> null
        }
    },
    restore = { saved ->
        if (saved.size == 2) {
            FormPhotoDestination.FormField(partId = saved[0], key = saved[1])
        } else {
            FormPhotoDestination.TicketAttachment(partId = saved[0])
        }
    }
)

@Composable
internal fun ChatbotContent(
    uiState: ChatbotUiState,
    historyPagingItems: LazyPagingItems<ChatBubbleUiModel>,
    hasNavigateBackIcon: Boolean,
    onInputChange: (String) -> Unit,
    onPostMessage: () -> Unit,
    onScrolledToMessage: () -> Unit,
    onSnackbarConsumed: () -> Unit,
    onSheetRequested: (SheetType) -> Unit,
    onSheetDismissed: () -> Unit,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteChatData: () -> Unit = {},
    onResetConversation: () -> Unit = {},
    onFormEnsure: (ConversationForm) -> Unit = {},
    onFormValueChange: (partId: String, key: String, value: String) -> Unit = { _, _, _ -> },
    onFormSubmit: (partId: String) -> Unit = {},
    onFormReload: (partId: String) -> Unit = {},
    offersAttachments: Boolean = false,
    onFormPhotoPicked: (FormPhotoDestination, open: () -> InputStream?) -> Unit = { _, _ -> },
    onFormFileRemoved: (partId: String, key: String) -> Unit = { _, _ -> },
    onTicketAttachmentRemoved: (partId: String) -> Unit = {},
    onUrlClick: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputBarHeightPx = remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val contentResolver = LocalContext.current.applicationContext.contentResolver
    // Saved so a pick still lands after the activity is recreated while the picker is open.
    var pendingPhotoDestination by rememberSaveable(stateSaver = PhotoDestinationSaver) {
        mutableStateOf<FormPhotoDestination?>(null)
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val destination = pendingPhotoDestination
        pendingPhotoDestination = null
        if (uri == null || destination == null) return@rememberLauncherForActivityResult
        onFormPhotoPicked(destination) { contentResolver.openInputStream(uri) }
    }
    val connectionState = uiState.session.connectionState
    val isSessionReady = connectionState is ChatConnectionState.Ready
    val showScrollToBottomButton = listState.canScrollBackward && !listState.isScrollInProgress
    val scrollToBottom: () -> Unit = { scope.launch { listState.animateScrollToItem(index = 0) } }
    val launchPhotoPicker: (FormPhotoDestination) -> Unit = { destination ->
        pendingPhotoDestination = destination
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // A sent message only reaches the list on the next composition, so scrolling from the send
    // itself would find nothing to scroll to.
    LaunchedEffect(uiState.messageToScrollTo) {
        if (uiState.messageToScrollTo == null) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(index = 0)
        onScrolledToMessage()
    }

    LaunchedEffect(uiState.snackbarState) {
        val snackbar = uiState.snackbarState ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = when (snackbar.type) {
                SnackbarType.StreamError -> snackbar.text.orEmpty()
                SnackbarType.MessageSendFailed -> resources.getString(R.string.chat_send_message_error)
                SnackbarType.DeleteChatDataSucceeded -> resources.getString(R.string.chat_delete_data_success)
                SnackbarType.DeleteChatDataFailed -> resources.getString(R.string.chat_delete_data_error)
                SnackbarType.ConversationResetFailed -> resources.getString(R.string.chat_reset_conversation_error)
            }
        )
        if (result == SnackbarResult.Dismissed) {
            onSnackbarConsumed()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentWindowInsets = WindowInsets(),
        containerColor = MaterialTheme.colorScheme.screenBackground,
        topBar = {
            ChatbotTopBar(
                assistantName = uiState.session.assistantName,
                hasNavigateBackIcon = hasNavigateBackIcon,
                showReset = connectionState !is ChatConnectionState.Connecting,
                isResetting = uiState.isResettingConversation,
                // Resetting mid-connect would restart a connection that is already running.
                isResetEnabled = isSessionReady,
                onReset = onResetConversation,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                ChatbotSnackbar(
                    message = data.visuals.message,
                    isError = uiState.snackbarState?.type?.isError == true
                )
            }
        },
        bottomBar = {
            if (isSessionReady) {
                ChatbotInputBar(
                    modifier = Modifier.measureInputBarHeight(inputBarHeightPx),
                    inputMessage = uiState.inputMessage,
                    isGenerating = uiState.pendingBotMessage != PendingBotMessageState.None,
                    isEnabled = !uiState.isResettingConversation,
                    onInputChange = onInputChange,
                    onPrivacyPolicyClick = { onSheetRequested(SheetType.Privacy) },
                    onSendMessage = {
                        keyboardController?.hide()
                        onPostMessage()
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (connectionState) {
                // A restart invalidates the session token, so the chat is as unusable as during
                // the first connect.
                is ChatConnectionState.Connecting,
                is ChatConnectionState.Restarting -> FullScreenLoader()

                is ChatConnectionState.Error,
                is ChatConnectionState.SessionExpired -> ChatbotErrorContent(onRetry = onRetry)

                is ChatConnectionState.Ready -> {
                    ConversationList(
                        liveMessages = uiState.conversation,
                        pendingBotMessageState = uiState.pendingBotMessage,
                        historyPagingItems = historyPagingItems,
                        welcomeMessage = uiState.session.welcomeMessage,
                        subtitle = uiState.session.subtitle,
                        botAvatarUrl = uiState.session.avatarUrl,
                        inputBarHeightPx = inputBarHeightPx,
                        listState = listState,
                        formCallbacks = FormUiCallbacks(
                            formModels = uiState.formModels,
                            isReplyBusy = uiState.pendingBotMessage != PendingBotMessageState.None ||
                                uiState.isEncodingAttachment,
                            offersAttachments = offersAttachments,
                            onEnsure = onFormEnsure,
                            onValueChange = onFormValueChange,
                            onSubmit = onFormSubmit,
                            onReload = onFormReload,
                            onPickFile = { partId, key ->
                                launchPhotoPicker(FormPhotoDestination.FormField(partId, key))
                            },
                            onRemoveFile = onFormFileRemoved,
                            onPickTicketAttachment = { partId ->
                                launchPhotoPicker(FormPhotoDestination.TicketAttachment(partId))
                            },
                            onRemoveTicketAttachment = onTicketAttachmentRemoved
                        ),
                        onUrlClick = onUrlClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    ScrollToBottomButton(
                        isVisible = showScrollToBottomButton,
                        onClick = scrollToBottom,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    )
                }
            }
        }
    }

    when (uiState.activeSheet) {
        SheetType.Privacy -> PrivacyPolicyBottomSheet(
            privacyPolicyUrl = uiState.session.privacyPolicyUrl,
            onPrivacyPolicyClick = { url ->
                if (onUrlClick != null) {
                    onUrlClick(url)
                } else {
                    uriHandler.openUri(url)
                }
            },
            onDeleteChatDataClick = { onSheetRequested(SheetType.DeleteChatData) },
            onDismiss = onSheetDismissed
        )

        SheetType.DeleteChatData -> ChatDataDeletionBottomSheet(
            isDeleting = uiState.isDeletingChatData,
            onDelete = onDeleteChatData,
            onCancel = onSheetDismissed,
            onDismiss = onSheetDismissed
        )

        null -> Unit
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatbotContentPreview(@PreviewParameter(ChatbotUiStatePreviewProvider::class) uiState: ChatbotUiState) {
    ChatbotTheme(theme = uiState.theme) {
        ChatbotContent(
            uiState = uiState,
            historyPagingItems = emptyEndOfPaginationHistory().collectAsLazyPagingItems(),
            hasNavigateBackIcon = true,
            onInputChange = {},
            onPostMessage = {},
            onScrolledToMessage = {},
            onSnackbarConsumed = {},
            onSheetRequested = {},
            onSheetDismissed = {},
            onNavigateBack = {},
            onRetry = {}
        )
    }
}
