package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.message.incoming.part.suggestion.SuggestionCard
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class SuggestionsPartBuilderTest {

    private val builder = SuggestionsPartBuilder()

    @Test
    fun `when several suggestions arrive expect the order the backend sent them in`() {
        builder.handle(sampleSuggestionAppended(id = "first", prompt = "One"))
        builder.handle(sampleSuggestionAppended(id = "second", prompt = "Two"))

        val suggestions = assertIs<MessagePartUiModel.Suggestions>(builder.build())
        assertEquals(
            expected = listOf("first", "second"),
            actual = suggestions.suggestions.map { it.id }
        )
        assertEquals(
            expected = listOf("One", "Two"),
            actual = suggestions.suggestions.map { it.promptText }
        )
    }

    @Test
    fun `when no suggestions arrived expect null`() {
        assertNull(builder.build())
    }

    @Test
    fun `when a delta is meant for another kind of delta part expect it ignored`() {
        builder.handle(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )

        assertNull(builder.build())
    }

    private fun sampleSuggestionAppended(
        id: String,
        prompt: String
    ) = StreamDeltaEvent.SuggestionAppended(
        partId = "part-1",
        suggestion = SuggestionCard(
            id = id,
            title = "Title $id",
            description = null,
            imageUrl = "https://example.com/$id.jpg",
            promptText = prompt
        )
    )
}
