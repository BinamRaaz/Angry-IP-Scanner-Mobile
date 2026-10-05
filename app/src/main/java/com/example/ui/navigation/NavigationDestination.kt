package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

/**
 * Top-level destinations in the app.
 */
enum class NavigationDestination(
    val titleRes: Int,
    val icon: ImageVector,
    val contentDescRes: Int
) {
    SCANNER(
        titleRes = R.string.nav_scanner,
        icon = Icons.Default.Radar,
        contentDescRes = R.string.cd_nav_scanner
    ),
    SPEED_TEST(
        titleRes = R.string.nav_speed_test,
        icon = Icons.Default.Speed,
        contentDescRes = R.string.cd_nav_speed_test
    ),
    HISTORY(
        titleRes = R.string.nav_history,
        icon = Icons.AutoMirrored.Filled.List,
        contentDescRes = R.string.cd_nav_history
    ),
    SETTINGS(
        titleRes = R.string.nav_settings,
        icon = Icons.Default.Settings,
        contentDescRes = R.string.cd_nav_settings
    ),
    ABOUT(
        titleRes = R.string.nav_about,
        icon = Icons.Default.Info,
        contentDescRes = R.string.cd_nav_about
    )
}
