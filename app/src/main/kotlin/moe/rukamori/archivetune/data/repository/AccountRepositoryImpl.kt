package moe.rukamori.archivetune.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.constants.AccountChannelHandleKey
import moe.rukamori.archivetune.constants.AccountEmailKey
import moe.rukamori.archivetune.constants.AccountNameKey
import moe.rukamori.archivetune.constants.DataSyncIdKey
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.constants.SavedAccountsKey
import moe.rukamori.archivetune.innertube.utils.hasYouTubeLoginCookie
import moe.rukamori.archivetune.utils.PreferenceStore
import moe.rukamori.archivetune.utils.SavedAccountCollection
import moe.rukamori.archivetune.utils.clearPlaybackAuthSession
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.decodeSavedAccounts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccountRepository {
    private val dataStore: DataStore<Preferences> = context.dataStore

    override fun getAccountInfo(): Flow<UserAccountInfo> {
        return dataStore.data.map { preferences ->
            val cookie = preferences[InnerTubeCookieKey].orEmpty()
            val isLoggedIn = hasYouTubeLoginCookie(cookie)
            val savedJson = preferences[SavedAccountsKey].orEmpty()
            val savedAccounts = SavedAccountCollection(decodeSavedAccounts(savedJson))

            UserAccountInfo(
                name = preferences[AccountNameKey].orEmpty(),
                email = preferences[AccountEmailKey].orEmpty(),
                handle = preferences[AccountChannelHandleKey].orEmpty(),
                avatarUrl = null,
                savedAccounts = savedAccounts,
                activeInnerTubeCookie = cookie,
                activeDataSyncId = preferences[DataSyncIdKey].orEmpty(),
                isLoggedIn = isLoggedIn
            )
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            PreferenceStore.launchEdit(context.dataStore) {
                clearPlaybackAuthSession(clearAccountIdentity = true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
