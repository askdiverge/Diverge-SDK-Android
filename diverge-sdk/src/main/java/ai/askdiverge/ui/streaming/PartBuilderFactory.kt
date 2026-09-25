package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.delta.DeltaPartType

internal fun interface PartBuilderFactory {
    fun create(partType: DeltaPartType): PartBuilder?
}

internal class DefaultPartBuilderFactory : PartBuilderFactory {
    override fun create(partType: DeltaPartType): PartBuilder? = when (partType) {
        DeltaPartType.RICH_TEXT -> RichTextPartBuilder()
        DeltaPartType.PRODUCTS -> ProductsPartBuilder()
        DeltaPartType.TABLE -> TablePartBuilder()
        DeltaPartType.UNKNOWN -> null
    }
}
