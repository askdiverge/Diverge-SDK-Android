package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.table.TableCellRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class TablePartRemote(
    val part_id: String,
    val caption: String?,
    val headers: List<TableCellRemote>,
    val alignments: List<TableColumnAlignmentRemote>?,
    val rows: List<List<TableCellRemote>>
) : MessagePartRemote
