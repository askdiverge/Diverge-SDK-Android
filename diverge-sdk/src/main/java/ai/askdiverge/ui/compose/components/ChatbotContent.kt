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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.state.SnackbarType
import ai.askdiverge.sdk.R
import kotlinx.coroutines.launch

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
    onUrlClick: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputBarHeightPx = remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val connectionState = uiState.session.connectionState
    val isSessionReady = connectionState is ChatConnectionState.Ready
    val showScrollToBottomButton = listState.canScrollBackward && !listState.isScrollInProgress
    val scrollToBottom: () -> Unit = { scope.launch { listState.animateScrollToItem(index = 0) } }

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
            onCancel = { onSheetRequested(SheetType.Privacy) },
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
