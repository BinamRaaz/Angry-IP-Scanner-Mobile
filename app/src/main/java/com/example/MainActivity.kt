package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.preferences.AppThemeMode
import com.example.core.preferences.DefaultPreferencesRepository
import com.example.core.preferences.ScannerPreferences
import com.example.core.preferences.scannerDataStore
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefRepo = remember { DefaultPreferencesRepository(scannerDataStore) }
            val preferences by prefRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = ScannerPreferences()
            )

            val darkTheme = when (preferences.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                HomeScreen()
            }
        }
    }
}
