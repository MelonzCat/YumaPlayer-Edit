package moe.rukamori.archivetune.ui.screens.settings

import moe.rukamori.archivetune.constants.AppFontPreference
import moe.rukamori.archivetune.data.repository.UserSettings

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val activeAccentHex: String = "ED5564",
    val fontPreference: AppFontPreference = AppFontPreference.DEFAULT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

fun UserSettings.toUiState() = SettingsUiState(
    isDarkTheme = this.isDarkTheme,
    activeAccentHex = this.themeColorHex.removePrefix("#"),
    fontPreference = this.fontPreference,
)
