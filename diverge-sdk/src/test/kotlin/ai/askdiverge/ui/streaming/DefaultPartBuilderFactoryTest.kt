package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.delta.DeltaPartType
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class DefaultPartBuilderFactoryTest {

    private val factory = DefaultPartBuilderFactory()

    @Test
    fun `when the delta part type is rich text expect the rich text builder`() {
        assertIs<RichTextPartBuilder>(factory.create(DeltaPartType.RICH_TEXT))
    }

    @Test
    fun `when the delta part type is products expect the products builder`() {
        assertIs<ProductsPartBuilder>(factory.create(DeltaPartType.PRODUCTS))
    }

    @Test
    fun `when the delta part type is table expect the table builder`() {
        assertIs<TablePartBuilder>(factory.create(DeltaPartType.TABLE))
    }

    @Test
    fun `when the delta part type is unknown expect no builder`() {
        assertNull(factory.create(DeltaPartType.UNKNOWN))
    }
}
