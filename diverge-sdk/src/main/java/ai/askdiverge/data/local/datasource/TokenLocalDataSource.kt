package ai.askdiverge.data.local.datasource

internal interface TokenLocalDataSource {
    fun getToken(): String?
    fun setToken(token: String)
    fun clear()
}
