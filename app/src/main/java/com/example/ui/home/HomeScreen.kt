package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.networking.NetworkConnectivityMonitor
import com.example.core.preferences.DefaultPreferencesRepository
import com.example.core.preferences.scannerDataStore
import com.example.data.local.AppDatabase
import com.example.data.repository.DefaultScanHistoryRepository
import com.example.data.repository.DefaultSpeedTestRepository
import com.example.ui.about.AboutScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.navigation.NavigationDestination
import com.example.ui.scanner.ScannerScreen
import com.example.ui.scanner.ScannerViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.speedtest.SpeedTestScreen
import com.example.ui.speedtest.SpeedTestViewModel

/**
 * Adaptive Home Screen supporting Window Size Classes:
 * - Compact (Portrait mobile): Material 3 Bottom NavigationBar.
 * - Medium / Expanded (Landscape, Tablets, Foldables): Material 3 NavigationRail
 *   with fluid centered max-width constraint (900.dp) to prevent awkward horizontal stretching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    scannerViewModel: ScannerViewModel = viewModel(),
    speedTestViewModel: SpeedTestViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentDestination by rememberSaveable { mutableStateOf(NavigationDestination.SCANNER) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scannerViewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        val historyRepo = DefaultScanHistoryRepository(db.scanHistoryDao())
        scannerViewModel.setScanHistoryRepository(historyRepo)
        val speedTestRepo = DefaultSpeedTestRepository(db.speedTestDao())
        speedTestViewModel.setRepository(speedTestRepo)
        val prefRepo = DefaultPreferencesRepository(context.scannerDataStore)
        scannerViewModel.setPreferencesRepository(prefRepo)
        val connectivityMonitor = NetworkConnectivityMonitor(context)
        scannerViewModel.observeConnectivity(connectivityMonitor)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp

        if (isExpanded) {
            // Adaptive Expanded Layout (Tablet, Foldable unfolded, Landscape)
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("app_navigation_rail"),
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_app_logo),
                                contentDescription = stringResource(R.string.app_short_title),
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.app_short_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    NavigationDestination.entries.forEach { destination ->
                        val selected = currentDestination == destination
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.contentDescRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(destination.titleRes))
                            },
                            modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 900.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        NavigationContent(
                            destination = currentDestination,
                            scannerViewModel = scannerViewModel,
                            speedTestViewModel = speedTestViewModel,
                            onNavigateToScanner = { currentDestination = NavigationDestination.SCANNER }
                        )
                    }
                }
            }
        } else {
            // Compact Phone Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_app_logo),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("app_top_bar")
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("app_bottom_bar")
                    ) {
                        NavigationDestination.entries.forEach { destination ->
                            val selected = currentDestination == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = stringResource(destination.contentDescRes)
                                    )
                                },
                                label = {
                                    Text(text = stringResource(destination.titleRes))
                                },
                                modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationContent(
                        destination = currentDestination,
                        scannerViewModel = scannerViewModel,
                        speedTestViewModel = speedTestViewModel,
                        onNavigateToScanner = { currentDestination = NavigationDestination.SCANNER }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationContent(
    destination: NavigationDestination,
    scannerViewModel: ScannerViewModel,
    speedTestViewModel: SpeedTestViewModel,
    onNavigateToScanner: () -> Unit
) {
    when (destination) {
        NavigationDestination.SCANNER -> {
            ScannerScreen(viewModel = scannerViewModel)
        }
        NavigationDestination.SPEED_TEST -> {
            SpeedTestScreen(
                onNavigateBack = onNavigateToScanner,
                viewModel = speedTestViewModel
            )
        }
        NavigationDestination.HISTORY -> {
            HistoryScreen(
                onNavigateBack = onNavigateToScanner,
                onRescanTarget = { startIp, endIp, cidr ->
                    scannerViewModel.loadTargetRange(startIp, endIp, cidr, startScanNow = true)
                    onNavigateToScanner()
                }
            )
        }
        NavigationDestination.SETTINGS -> {
            SettingsScreen(
                onNavigateBack = onNavigateToScanner
            )
        }
        NavigationDestination.ABOUT -> {
            AboutScreen(
                onNavigateBack = onNavigateToScanner
            )
        }
    }
}
