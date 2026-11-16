package ai.askdiverge.data.model.message

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.model.message.incoming.part.SuggestionsPartRemote
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.domain.model.message.incoming.part.SuggestionsPart
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SuggestionsPartDecodingTest {

    private val moshi = Moshi.Builder().buildConversationModels().build()
    private val partAdapter = moshi.adapter(MessagePartRemote::class.java)

    @Test
    fun `a suggestions part decodes its cards in order`() {
        val remote = assertIs<SuggestionsPartRemote>(
            partAdapter.fromJson(
                """
                {
                  "type": "suggestions",
                  "part_id": "sug-1",
                  "suggestions": [
                    {
                      "id": "a",
                      "title": "Boots",
                      "description": "Black",
                      "image_url": "https://example.com/a.jpg",
                      "prompt_text": "Show boots"
                    },
                    {
                      "id": "b",
                      "title": "Help",
                      "image_url": "",
                      "prompt_text": "I need help"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val domain = assertIs<SuggestionsPart>(remote.mapToDomain())
        assertEquals("sug-1", domain.id)
        assertEquals(listOf("a", "b"), domain.suggestions.map { it.id })
        assertEquals("Show boots", domain.suggestions[0].promptText)
        assertNull(domain.suggestions[1].description)
    }

    @Test
    fun `an unknown part type still decodes as null remote`() {
        assertNull(
            partAdapter.fromJson(
                """
                { "type": "not_a_real_part", "part_id": "x" }
                """.trimIndent()
            )
        )
    }
}
