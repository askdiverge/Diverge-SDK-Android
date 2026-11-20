package ai.askdiverge.data.model.message

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.model.event.delta.DeltaActionRemote
import ai.askdiverge.data.model.event.delta.DeltaPartTypeRemote
import ai.askdiverge.data.model.event.delta.DeltaRemote
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
    fun `a streamed suggestions part decodes its start and append wire values`() {
        val deltaAdapter = moshi.adapter(DeltaRemote::class.java)

        val start = deltaAdapter.fromJson("""{ "action": "start_part", "part_type": "suggestions" }""")
        val append = deltaAdapter.fromJson(
            """
            {
              "action": "append_suggestion",
              "suggestion": {
                "id": "a",
                "title": "Boots",
                "image_url": "https://example.com/a.jpg",
                "prompt_text": "Show boots"
              }
            }
            """.trimIndent()
        )

        assertEquals(DeltaPartTypeRemote.SUGGESTIONS, start?.part_type)
        assertEquals(DeltaActionRemote.APPEND_SUGGESTION, append?.action)
        assertEquals("Show boots", append?.suggestion?.prompt_text)
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
