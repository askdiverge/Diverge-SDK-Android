package ai.askdiverge.data.local

import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TokenLocalDataSourceImplTest {

    private val tokenLocalDataSource = TokenLocalDataSourceImpl()

    @Test
    fun `when a token is set expect it to be read back`() {
        tokenLocalDataSource.setToken("token")

        assertEquals(
            expected = "token",
            actual = tokenLocalDataSource.getToken()
        )
    }

    @Test
    fun `when a second token is set expect it to replace the first`() {
        tokenLocalDataSource.setToken("first-token")

        tokenLocalDataSource.setToken("second-token")

        assertEquals(
            expected = "second-token",
            actual = tokenLocalDataSource.getToken()
        )
    }

    @Test
    fun `when the token is cleared expect no token behind`() {
        tokenLocalDataSource.setToken("token")

        tokenLocalDataSource.clear()

        assertNull(tokenLocalDataSource.getToken())
    }
}
