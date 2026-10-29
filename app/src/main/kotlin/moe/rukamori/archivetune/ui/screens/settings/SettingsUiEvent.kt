package moe.rukamori.archivetune.ui.screens.settings

import moe.rukamori.archivetune.constants.AppFontPreference

sealed interface SettingsUiEvent {
    data class SelectAccentColor(val colorHex: String) : SettingsUiEvent
    data class SelectFont(val preference: AppFontPreference) : SettingsUiEvent
    object DismissError : SettingsUiEvent
}
