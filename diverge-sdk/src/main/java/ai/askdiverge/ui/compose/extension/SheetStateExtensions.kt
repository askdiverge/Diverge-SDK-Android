package ai.askdiverge.ui.compose.extension

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Hides the sheet with its exit animation before invoking [onHidden].
 * Removing a ModalBottomSheet from composition by state change alone skips its exit animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun SheetState.hideWithAnimation(
    scope: CoroutineScope,
    onHidden: () -> Unit
) {
    scope.launch {
        hide()
    }.invokeOnCompletion {
        if (!isVisible) {
            onHidden()
        }
    }
}
