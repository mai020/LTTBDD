package com.example.moneykeep

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moneykeep.data.local.AppDatabase
import com.example.moneykeep.navigation.AppNavigation
import com.example.moneykeep.ui.theme.MoneyKeepTheme
import com.example.moneykeep.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MoneyKeepLifecycle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate()")

        enableEdgeToEdge()

        setContent {
            val db = AppDatabase.getDatabase(this)
            val themeViewModel: ThemeViewModel =
                viewModel(factory = ThemeViewModel.Factory(db))

            val isDarkModeOverride by themeViewModel.isDarkMode.collectAsState()
            val darkTheme = isDarkModeOverride ?: isSystemInDarkTheme()

            MoneyKeepTheme(darkTheme = darkTheme) {
                AppNavigation(themeViewModel = themeViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }
}