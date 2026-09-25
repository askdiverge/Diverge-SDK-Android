package ai.askdiverge.ui.compose.components.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.icons.ChatbotIcons
import ai.askdiverge.ui.compose.theme.screenBackground

private const val COLLAPSED_SCALE = 0.4f

@Composable
internal fun ScrollToBottomButton(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            initialScale = COLLAPSED_SCALE
        ),
        exit = fadeOut() + scaleOut(targetScale = COLLAPSED_SCALE)
    ) {
        ScrollToBottomFab(onClick = onClick)
    }
}

@Composable
private fun ScrollToBottomFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = CircleShape
        ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.screenBackground,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Icon(
            imageVector = ChatbotIcons.ArrowDown,
            contentDescription = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScrollToBottomButtonPreview() {
    ScrollToBottomButton(
        isVisible = true,
        onClick = {}
    )
}
