package ai.askdiverge.ui.compose.components.message.suggestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.model.message.part.suggestion.SuggestionCardUiModel

private const val COLUMN_COUNT = 2
private val GRID_HORIZONTAL_GAP = 6.dp
private val GRID_VERTICAL_GAP = 16.dp

@Composable
internal fun SuggestionsContent(
    suggestions: List<SuggestionCardUiModel>,
    onSuggestionClick: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = GRID_VERTICAL_GAP)
    ) {
        suggestions.chunked(size = COLUMN_COUNT).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = GRID_HORIZONTAL_GAP)
            ) {
                row.forEach { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        enabled = enabled,
                        onClick = onSuggestionClick,
                        modifier = Modifier.weight(weight = 1f)
                    )
                }
                if (row.size < COLUMN_COUNT) {
                    Spacer(modifier = Modifier.weight(weight = 1f))
                }
            }
        }
    }
}
