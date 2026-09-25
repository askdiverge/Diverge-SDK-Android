package ai.askdiverge.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.ui.ChatbotViewModel
import ai.askdiverge.ui.compose.components.ChatbotContent
import ai.askdiverge.ui.compose.theme.ChatbotTheme
import ai.askdiverge.ui.compose.theme.screenBackground

private val SHEET_CORNER_RADIUS = 28.dp

/**
 * The chatbot conversation presented as a modal bottom sheet.
 *
 * @param chatbotCallbacks the host's implementation of the actions the chatbot delegates to it.
 * @param onDismiss called when the sheet has been dismissed.
 * @param modifier applied to the sheet.
 * @param currentPage the page the sheet was opened from, sent along with each message.
 * @param fontFamily the font the chat draws in. The host's own is used when null, which needs a
 * Material 3 theme in scope; pass one to set it from a Material 2 or unthemed host.
 * @param onUrlClick handles links in messages. Links open in the platform handler when null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotBottomSheet(
    chatbotCallbacks: ChatbotCallbacks,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.onSheetOpened() }

    ChatbotTheme(
        theme = uiState.theme,
        fontFamily = fontFamily
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = SHEET_CORNER_RADIUS,
                topEnd = SHEET_CORNER_RADIUS
            ),
            containerColor = MaterialTheme.colorScheme.screenBackground,
            tonalElevation = 0.dp,
            // The top inset is already applied by statusBarsPadding() on the sheet itself.
            contentWindowInsets = {
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            },
            modifier = modifier.statusBarsPadding()
        ) {
            ChatbotContent(
                uiState = uiState,
                historyPagingItems = historyPagingItems,
                hasNavigateBackIcon = false,
                onInputChange = viewModel::onInputChanged,
                onPostMessage = viewModel::postMessage,
                onScrolledToMessage = viewModel::onScrolledToMessage,
                onSnackbarConsumed = viewModel::onSnackbarConsumed,
                onSheetRequested = viewModel::onSheetRequested,
                onSheetDismissed = viewModel::onSheetDismissed,
                onNavigateBack = onDismiss,
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize(),
                onDeleteChatData = viewModel::deleteChatData,
                onResetConversation = viewModel::resetConversation,
                onUrlClick = onUrlClick
            )
        }
    }
}
