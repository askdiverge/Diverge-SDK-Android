package ai.askdiverge.ui.compose.components.conversation.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.components.message.BotMessageBubble
import ai.askdiverge.ui.compose.previewprovider.emptyEndOfPaginationHistory
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.SubtitleUiModel
import ai.askdiverge.sdk.R

internal fun LazyListScope.conversationHeader(
    historyPagingItems: LazyPagingItems<ChatBubbleUiModel>,
    welcomeMessage: String,
    subtitle: SubtitleUiModel?,
    botAvatarUrl: String,
    onUrlClick: ((String) -> Unit)? = null
) {
    val refresh = historyPagingItems.loadState.refresh
    val append = historyPagingItems.loadState.append
    when {
        refresh is LoadState.Loading || append is LoadState.Loading -> item {
            HistoryLoadingIndicator()
        }

        refresh is LoadState.Error || append is LoadState.Error -> item {
            HistoryLoadError(onRetry = historyPagingItems::retry)
        }

        append.endOfPaginationReached -> {
            if (welcomeMessage.isNotBlank()) {
                item {
                    WelcomeMessage(
                        text = welcomeMessage,
                        botAvatarUrl = botAvatarUrl
                    )
                }
            }

            subtitle?.let {
                subtitleBanner(
                    uiModel = it,
                    onUrlClick = onUrlClick
                )
            }
        }
    }
}

@Composable
private fun HistoryLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
    )
}

@Composable
private fun HistoryLoadError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ChatbotIcons.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.chat_history_load_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_history_load_error_retry),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun WelcomeMessage(
    text: String,
    botAvatarUrl: String
) {
    BotMessageBubble(avatarUrl = botAvatarUrl, isStreaming = false) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationHeaderPreview() {
    val historyPagingItems = emptyEndOfPaginationHistory().collectAsLazyPagingItems()
    LazyColumn {
        conversationHeader(
            historyPagingItems = historyPagingItems,
            welcomeMessage = "Hi! How can I help you?",
            subtitle = SubtitleUiModel(
                text = "You are communicating with an artificial intelligence.",
                link = null
            ),
            botAvatarUrl = ""
        )
    }
}
