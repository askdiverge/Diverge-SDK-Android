package ai.askdiverge.ui.compose.components.message.table

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import ai.askdiverge.ui.compose.components.message.TextContent
import ai.askdiverge.ui.compose.previewprovider.TablePreviewProvider
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellBlockUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellUiModel
import ai.askdiverge.ui.model.message.part.table.TableColumnAlignmentUiModel

private const val IMAGE_RATIO = 0.7f
private val BORDER_WIDTH = 1.dp

@Composable
internal fun TableContent(
    content: MessagePartUiModel.Table,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null
) {
    val rows = listOfNotNull(content.headers.takeIf { it.isNotEmpty() }) + content.rows

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        content.caption?.let { caption ->
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .border(
                    width = BORDER_WIDTH,
                    color = MaterialTheme.colorScheme.outline
                )
        ) {
            rows.forEachIndexed { index, cells ->
                TableRow(
                    cells = cells,
                    hasTopBorder = index > 0,
                    onUrlClick = onUrlClick
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<TableCellUiModel>,
    hasTopBorder: Boolean,
    onUrlClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier.rowBorders(
            color = MaterialTheme.colorScheme.outline,
            columnCount = cells.size,
            hasTopBorder = hasTopBorder
        )
    ) {
        cells.forEach { cell ->
            TableCell(
                cell = cell,
                modifier = Modifier.weight(weight = 1f),
                onUrlClick = onUrlClick
            )
        }
    }
}

@Composable
private fun TableCell(
    cell: TableCellUiModel,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = cell.alignment.toHorizontalAlignment()
    ) {
        cell.blocks.forEach { block ->
            when (block) {
                is TableCellBlockUiModel.Text -> TextContent(
                    content = block.content,
                    role = MessageRoleUiModel.BOT,
                    onUrlClick = onUrlClick
                )

                is TableCellBlockUiModel.Image -> CellImage(image = block)
            }
        }
    }
}

@Composable
private fun CellImage(image: TableCellBlockUiModel.Image) {
    AsyncImage(
        modifier = Modifier
            .aspectRatio(ratio = IMAGE_RATIO)
            .fillMaxWidth(),
        model = image.url,
        contentDescription = image.alt,
        imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
        contentScale = ContentScale.FillWidth
    )
}

/**
 * Handles the creation of vertical borders between the columns
 * in a table row.
 * If [hasTopBorder] is true, the top border will also be drawn.
 */
private fun Modifier.rowBorders(
    color: Color,
    columnCount: Int,
    hasTopBorder: Boolean
): Modifier = drawBehind {
    val stroke = BORDER_WIDTH.toPx()
    if (hasTopBorder) {
        drawLine(
            color = color,
            start = Offset(x = 0f, y = stroke / 2),
            end = Offset(x = size.width, y = stroke / 2),
            strokeWidth = stroke
        )
    }

    // Draw border between each column
    for (column in 1 until columnCount) {
        val x = size.width * column / columnCount
        drawLine(
            color = color,
            start = Offset(x = x, y = 0f),
            end = Offset(x = x, y = size.height),
            strokeWidth = stroke
        )
    }
}

private fun TableColumnAlignmentUiModel.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    TableColumnAlignmentUiModel.LEFT -> Alignment.Start
    TableColumnAlignmentUiModel.CENTER -> Alignment.CenterHorizontally
    TableColumnAlignmentUiModel.RIGHT -> Alignment.End
}

@Preview(showBackground = true)
@Composable
private fun TableContentPreview(@PreviewParameter(TablePreviewProvider::class) content: MessagePartUiModel.Table) {
    TableContent(content = content)
}
