package ai.askdiverge.data.model.message.incoming.part.table

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.text.TableCellBlockRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class TableCellRemote(val blocks: List<TableCellBlockRemote>)
