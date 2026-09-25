package ai.askdiverge.data.local.datasource

internal class TokenLocalDataSourceImpl : TokenLocalDataSource {
    @Volatile private var token: String? = null

    override fun getToken(): String? = token

    override fun setToken(token: String) {
        this.token = token
    }

    override fun clear() {
        token = null
    }
}
