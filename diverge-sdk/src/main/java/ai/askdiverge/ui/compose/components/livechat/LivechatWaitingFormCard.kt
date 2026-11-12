package ai.askdiverge.ui.compose.components.livechat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.askdiverge.sdk.R
import ai.askdiverge.ui.compose.components.message.form.FormFieldControl
import ai.askdiverge.ui.compose.components.message.form.formBannerErrorText
import ai.askdiverge.ui.compose.components.message.form.formFieldErrorText
import ai.askdiverge.ui.compose.theme.assistantBubbleBackground
import ai.askdiverge.ui.compose.theme.assistantBubbleBorder
import ai.askdiverge.ui.compose.theme.assistantMessageText
import ai.askdiverge.ui.compose.theme.mutedText
import ai.askdiverge.ui.form.FormValidator
import ai.askdiverge.ui.form.WaitingFormModel
import androidx.compose.ui.semantics.LiveRegionMode

private val CARD_MAX_HEIGHT = 280.dp

/** Share of the height above the keyboard the scrollable fields may take. */
private const val FIELDS_HEIGHT_SHARE = 0.25f
private val FIELD_SCROLL_STEP = 96.dp

/**
 * Session-scoped “Add details for the agent” card while livechat status is `waiting`.
 * Save chrome stays pinned under the scrollable fields.
 */
@Composable
internal fun LivechatWaitingFormCard(
    model: WaitingFormModel,
    onValueChange: (key: String, value: String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(R.string.livechat_waiting_form_title)
    val description = stringResource(R.string.livechat_waiting_form_description)
    val savedCopy = stringResource(R.string.livechat_waiting_form_saved)
    val failedCopy = stringResource(R.string.livechat_waiting_form_failed)
    val visibleFields = FormValidator.visibleFields(model.form, model.values)
    val scrollState = rememberScrollState()
    val background = MaterialTheme.colorScheme.assistantBubbleBackground
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val heightAboveKeyboard =
        with(density) { (windowHeightPx - WindowInsets.ime.getBottom(density)).toDp() }
    val fieldsMaxHeight =
        minOf(CARD_MAX_HEIGHT, heightAboveKeyboard * FIELDS_HEIGHT_SHARE).coerceAtLeast(0.dp)

    LaunchedEffect(model.fieldErrorReveals, model.firstFieldWithError) {
        val key = model.firstFieldWithError ?: return@LaunchedEffect
        val index = visibleFields.indexOfFirst { it.key == key }
        if (index >= 0) {
            // Approximate scroll: each field is tall enough that index * 96 dp brings it into view.
            scrollState.animateScrollTo(with(density) { (FIELD_SCROLL_STEP * index).roundToPx() })
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.assistantBubbleBorder)
            .background(background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.assistantMessageText,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.mutedText
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = fieldsMaxHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .drawWithContent {
                        drawContent()
                        if (scrollState.canScrollForward) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, background),
                                    startY = size.height - 48.dp.toPx(),
                                    endY = size.height
                                )
                            )
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleFields.forEach { field ->
                    FormFieldControl(
                        field = field,
                        value = model.values[field.key].orEmpty(),
                        error = model.fieldErrors[field.key],
                        isEnabled = !model.isSaving,
                        onValueChange = { onValueChange(field.key, it) },
                        offersAttachments = false
                    )
                    model.fieldErrors[field.key]?.let { error ->
                        Text(
                            text = formFieldErrorText(error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        model.formError?.let { error ->
            Text(
                text = formBannerErrorText(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                }
            )
        }

        val saveLabel = if (model.isSaving) {
            stringResource(R.string.livechat_waiting_form_saving)
        } else {
            stringResource(R.string.livechat_waiting_form_save)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSave,
                enabled = model.canSave,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = saveLabel }
            ) {
                if (model.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = saveLabel)
                }
            }

            when {
                model.isSaved -> Text(
                    text = savedCopy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.mutedText,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = savedCopy
                    }
                )
                model.phase is WaitingFormModel.Phase.Failed -> Text(
                    text = failedCopy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = failedCopy
                    }
                )
            }
        }
    }
}
