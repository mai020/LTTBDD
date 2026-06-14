package com.example.moneykeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.moneykeep.data.local.AppDatabase
import com.example.moneykeep.data.local.UserSetting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val database: AppDatabase) : ViewModel() {

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
    }

    // null means follow system, "true" means dark, "false" means light
    val isDarkMode: StateFlow<Boolean?> = database.userSettingDao()
        .getSettingFlow(KEY_DARK_MODE)
        .map { value ->
            when (value) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleDarkMode(enabled: Boolean?) {
        viewModelScope.launch {
            val value = enabled?.toString() ?: "system"
            database.userSettingDao().saveSetting(UserSetting(KEY_DARK_MODE, value))
        }
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemeViewModel(database) as T
        }
    }
}
