package ai.askdiverge.data.model.event.delta

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.product.ProductCardRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableCellRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListItemRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class DeltaRemote(
    val action: DeltaActionRemote,
    val part_type: DeltaPartTypeRemote? = null,
    val block_index: Int? = null,
    val block_type: DeltaBlockTypeRemote? = null,
    val text: String? = null,
    val span: RichTextSpanRemote? = null,
    val item: RichTextBulletListItemRemote? = null,
    val product: ProductCardRemote? = null,
    val caption: String? = null,
    val headers: List<TableCellRemote>? = null,
    val alignments: List<TableColumnAlignmentRemote>? = null,
    val row: List<TableCellRemote>? = null
)
