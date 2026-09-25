package ai.askdiverge.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.ui.ChatbotViewModel
import ai.askdiverge.ui.compose.components.ChatbotContent
import ai.askdiverge.ui.compose.theme.ChatbotTheme

/**
 * The chatbot conversation presented as a full screen.
 *
 * @param chatbotCallbacks the host's implementation of the actions the chatbot delegates to it.
 * @param onNavigateBack called when the back control is used.
 * @param modifier applied to the screen.
 * @param hasNavigateBackIcon whether the back control is shown.
 * @param currentPage the page the chat was opened from, sent along with each message.
 * @param fontFamily the font the chat draws in. The host's own is used when null, which needs a
 * Material 3 theme in scope; pass one to set it from a Material 2 or unthemed host.
 * @param onUrlClick handles links in messages. Links open in the platform handler when null.
 */
@Composable
fun ChatbotScreen(
    chatbotCallbacks: ChatbotCallbacks,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    hasNavigateBackIcon: Boolean = true,
    currentPage: String? = null,
    fontFamily: FontFamily? = null,
    onUrlClick: ((String) -> Unit)? = null
) {
    val viewModel: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.factory(
            chatbotCallbacks = chatbotCallbacks,
            currentPage = currentPage
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyPagingItems = viewModel.historyMessages.collectAsLazyPagingItems()

    ChatbotTheme(
        theme = uiState.theme,
        fontFamily = fontFamily
    ) {
        ChatbotContent(
            modifier = modifier,
            uiState = uiState,
            historyPagingItems = historyPagingItems,
            hasNavigateBackIcon = hasNavigateBackIcon,
            onInputChange = viewModel::onInputChanged,
            onPostMessage = viewModel::postMessage,
            onScrolledToMessage = viewModel::onScrolledToMessage,
            onSnackbarConsumed = viewModel::onSnackbarConsumed,
            onSheetRequested = viewModel::onSheetRequested,
            onSheetDismissed = viewModel::onSheetDismissed,
            onNavigateBack = onNavigateBack,
            onRetry = viewModel::retry,
            onDeleteChatData = viewModel::deleteChatData,
            onResetConversation = viewModel::resetConversation,
            onUrlClick = onUrlClick
        )
    }
}
