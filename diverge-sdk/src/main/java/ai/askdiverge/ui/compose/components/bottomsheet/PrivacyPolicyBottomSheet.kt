package ai.askdiverge.ui.compose.components.bottomsheet

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.extension.hideWithAnimation
import ai.askdiverge.sdk.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivacyPolicyBottomSheet(
    privacyPolicyUrl: String,
    onPrivacyPolicyClick: (String) -> Unit,
    onDeleteChatDataClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.chat_privacy_policy_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        PrivacyPolicySheetRow(
            leadingIcon = ChatbotIcons.ShieldCheck,
            text = stringResource(R.string.chat_privacy_policy_row_privacy_policy),
            trailingIcon = ChatbotIcons.ArrowUpRight,
            contentColor = MaterialTheme.colorScheme.onBackground,
            onClick = {
                sheetState.hideWithAnimation(
                    scope = scope,
                    onHidden = {
                        onDismiss()
                        onPrivacyPolicyClick(privacyPolicyUrl)
                    }
                )
            }
        )

        PrivacyPolicySheetRow(
            leadingIcon = ChatbotIcons.Trash,
            text = stringResource(R.string.chat_privacy_policy_row_delete_data),
            trailingIcon = ChatbotIcons.ChevronRight,
            contentColor = MaterialTheme.colorScheme.error,
            onClick = {
                sheetState.hideWithAnimation(
                    scope = scope,
                    onHidden = onDeleteChatDataClick
                )
            }
        )

        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Preview
@Composable
private fun PrivacyPolicyBottomSheetPreview() {
    PrivacyPolicyBottomSheet(
        privacyPolicyUrl = "https://www.boozt.com/privacy-policy",
        onPrivacyPolicyClick = {},
        onDeleteChatDataClick = {},
        onDismiss = {}
    )
}
