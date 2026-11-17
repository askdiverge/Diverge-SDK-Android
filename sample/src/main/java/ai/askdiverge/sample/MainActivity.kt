package ai.askdiverge.sample

import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.LivechatSessionInfo
import ai.askdiverge.ui.compose.ChatbotBottomSheet
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Debug builds talk to the development API, release builds to production. */
private val environment =
    if (BuildConfig.DEBUG) DivergeEnvironment.DEVELOPMENT else DivergeEnvironment.PRODUCTION

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen()
                }
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    var token by rememberSaveable { mutableStateOf("") }
    var isChatPresented by rememberSaveable { mutableStateOf(false) }
    // Last livechat phase the SDK reported while the sheet was open (host-badge recipe).
    var livechatSessionLabel by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }
        )
        Text(text = stringResource(R.string.instructions))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(stringResource(R.string.token_label)) },
            supportingText = { Text(stringResource(R.string.token_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { isChatPresented = true },
            enabled = token.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.open_chat))
        }
        livechatSessionLabel?.let { label ->
            Text(
                text = stringResource(R.string.livechat_session_label, label),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // The SDK renders only the conversation; presenting and dismissing it is the host's job.
    if (isChatPresented) {
        ChatbotBottomSheet(
            chatbotCallbacks = SampleCallbacks(
                token = { token },
                reportLivechatSession = { info ->
                    livechatSessionLabel = info.toSampleLabel()
                }
            ),
            onDismiss = { isChatPresented = false },
            environment = environment
        )
    }
}

private fun LivechatSessionInfo.toSampleLabel(): String {
    val name = agentDisplayName
    return if (status == LivechatSessionInfo.Status.ACTIVE && name != null) {
        "${status.name.lowercase()} ($name)"
    } else {
        status.name.lowercase()
    }
}

/**
 * Token minting and data lifecycle stay with the host; the SDK only calls back for them. The
 * token is read on each call because the chat outlives one opening of the sheet.
 */
private class SampleCallbacks(
    private val token: () -> String,
    private val reportLivechatSession: (LivechatSessionInfo) -> Unit = {}
) : ChatbotCallbacks {
    override suspend fun fetchToken(): Result<String> = Result.success(token())

    override suspend fun createNewToken(): Result<String> = Result.success(token())

    override suspend fun deleteChatData(): Result<Unit> = Result.success(Unit)

    override fun onLivechatSessionChange(info: LivechatSessionInfo) = reportLivechatSession(info)
}
