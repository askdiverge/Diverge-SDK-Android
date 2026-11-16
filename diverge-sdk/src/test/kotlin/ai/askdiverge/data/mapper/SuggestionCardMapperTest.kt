package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.suggestion.SuggestionCardRemote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SuggestionCardMapperTest {

    @Test
    fun `maps all fields correctly`() {
        val remote = SuggestionCardRemote(
            id = "card-1",
            title = "Find boots",
            description = "Black ankle boots",
            image_url = "https://example.com/boots.jpg",
            prompt_text = "Show me black boots"
        )

        val result = remote.mapToDomain()

        assertEquals("card-1", result.id)
        assertEquals("Find boots", result.title)
        assertEquals("Black ankle boots", result.description)
        assertEquals("https://example.com/boots.jpg", result.imageUrl)
        assertEquals("Show me black boots", result.promptText)
    }

    @Test
    fun `a missing description stays null`() {
        val remote = SuggestionCardRemote(
            id = "card-2",
            title = "Help",
            image_url = "",
            prompt_text = "I need help"
        )

        assertNull(remote.mapToDomain().description)
        assertEquals("", remote.mapToDomain().imageUrl)
    }
}
