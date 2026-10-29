package moe.rukamori.archivetune.data.repository

import kotlinx.coroutines.flow.Flow
import moe.rukamori.archivetune.utils.SavedAccountCollection

data class UserAccountInfo(
    val name: String,
    val email: String,
    val handle: String,
    val avatarUrl: String?,
    val savedAccounts: SavedAccountCollection,
    val activeInnerTubeCookie: String,
    val activeDataSyncId: String,
    val isLoggedIn: Boolean
)

interface AccountRepository {
    fun getAccountInfo(): Flow<UserAccountInfo>
    suspend fun logout(): Result<Unit>
}
