package com.example.ui.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.fetchers.FetcherId
import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.core.networking.AndroidNetworkInfoProvider
import com.example.core.utilities.PortUtils
import com.example.ui.components.HostDetailSheet
import com.example.ui.components.HostStatusBadge
import com.example.ui.export.ExportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val networkInfoProvider = remember { AndroidNetworkInfoProvider(context) }
    val networkState by viewModel.networkState.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showColumnsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedHostForDetails by remember { mutableStateOf<DiscoveredHost?>(null) }
    var showCancelScanDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = progress.isScanning || selectedHostForDetails != null) {
        if (selectedHostForDetails != null) {
            selectedHostForDetails = null
        } else if (progress.isScanning) {
            showCancelScanDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshNetworkState(networkInfoProvider)
    }

    LaunchedEffect(progress.isScanning) {
        if (!progress.isScanning && progress.scannedCount > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    PullToRefreshBox(
        isRefreshing = progress.isScanning,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.startScan()
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("scanner_pull_to_refresh")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Local Network / Wi-Fi Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_network_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (networkState.isWifi) Icons.Default.Wifi else Icons.Default.Lan,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = networkState.networkName ?: stringResource(R.string.network_disconnected),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (networkState.isConnected && networkState.localIp != null) {
                                Text(
                                    text = "IP: ${networkState.localIp} • Mask: ${networkState.subnetMask ?: "255.255.255.0"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                networkState.gatewayIp?.let { gw ->
                                    Text(
                                        text = "Gateway: $gw",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    if (networkState.isConnected) {
                        Button(
                            onClick = { viewModel.scanCurrentSubnet(networkInfoProvider) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("scan_current_subnet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.btn_scan_current_subnet),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ip_target_config_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with Mode Switch & Columns Config
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.scanner_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showColumnsDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("customize_columns_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewColumn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_customize_columns),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleCidrMode() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("toggle_cidr_mode_button")
                            ) {
                                Text(
                                    text = if (uiState.isCidrMode) stringResource(R.string.mode_range) else stringResource(R.string.mode_cidr),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Input Fields
                    if (uiState.isCidrMode) {
                        OutlinedTextField(
                            value = uiState.cidrInput,
                            onValueChange = { viewModel.onCidrChanged(it) },
                            label = { Text(stringResource(R.string.label_cidr)) },
                            singleLine = true,
                            enabled = !progress.isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cidr_input")
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.startIp,
                                onValueChange = { viewModel.onStartIpChanged(it) },
                                label = { Text(stringResource(R.string.label_start_ip)) },
                                singleLine = true,
                                enabled = !progress.isScanning,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_ip_input")
                            )

                            OutlinedTextField(
                                value = uiState.endIp,
                                onValueChange = { viewModel.onEndIpChanged(it) },
                                label = { Text(stringResource(R.string.label_end_ip)) },
                                singleLine = true,
                                enabled = !progress.isScanning,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("end_ip_input")
                            )
                        }
                    }

                    // Error Display
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        uiState.errorMessage?.let { errorMsg ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                                    .testTag("scan_error_message")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Main Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (progress.isScanning) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.stopScan()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("stop_scan_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_stop_scan),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.startScan()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_scan_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_start_scan),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time Progress Card
            AnimatedVisibility(visible = progress.isScanning || progress.scannedCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_progress_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dotColor by animateColorAsState(
                                    targetValue = if (progress.isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    animationSpec = tween(300),
                                    label = "dot_color"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (progress.isScanning) {
                                        "Scanning… ${(progress.progressFraction * 100).toInt()}%"
                                    } else {
                                        "Scan Complete"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "${progress.scannedCount} / ${progress.totalCount}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("scan_progress_indicator"),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.stat_alive_format, progress.aliveCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.stat_dead_format, progress.deadCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            progress.currentIp?.let { current ->
                                Text(
                                    text = "Target: $current",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchExpanded) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    isSearchExpanded = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_search_clear)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("results_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hosts (${results.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        FilterChip(
                            selected = uiState.showOnlyAlive,
                            onClick = { viewModel.toggleShowOnlyAlive() },
                            label = { Text(stringResource(R.string.filter_alive_only)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_alive_chip")
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (results.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showExportDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("export_results_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_export_action),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search_hint)
                            )
                        }
                    }
                }
            }

            // Sort Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SortChip(
                    label = stringResource(R.string.sort_ip),
                    isSelected = uiState.sortOption == SortOption.IP_ASC || uiState.sortOption == SortOption.IP_DESC,
                    isAscending = uiState.sortOption == SortOption.IP_ASC,
                    onClick = {
                        val next = if (uiState.sortOption == SortOption.IP_ASC) SortOption.IP_DESC else SortOption.IP_ASC
                        viewModel.onSortOptionChanged(next)
                    },
                    tag = "sort_ip_chip"
                )

                SortChip(
                    label = stringResource(R.string.sort_ping),
                    isSelected = uiState.sortOption == SortOption.PING_ASC,
                    isAscending = true,
                    onClick = { viewModel.onSortOptionChanged(SortOption.PING_ASC) },
                    tag = "sort_ping_chip"
                )

                SortChip(
                    label = stringResource(R.string.sort_status),
                    isSelected = uiState.sortOption == SortOption.STATUS_ALIVE_FIRST,
                    isAscending = true,
                    onClick = { viewModel.onSortOptionChanged(SortOption.STATUS_ALIVE_FIRST) },
                    tag = "sort_status_chip"
                )

                SortChip(
                    label = stringResource(R.string.sort_hostname),
                    isSelected = uiState.sortOption == SortOption.HOSTNAME_ASC,
                    isAscending = true,
                    onClick = { viewModel.onSortOptionChanged(SortOption.HOSTNAME_ASC) },
                    tag = "sort_host_chip"
                )
            }

            // Dynamic Results Table Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.header_index),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(36.dp)
                            .testTag("header_index")
                    )
                    Text(
                        text = stringResource(R.string.header_ip),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = stringResource(R.string.header_status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.8f)
                    )
                    if (FetcherId.PING in uiState.enabledFetchers) {
                        Text(
                            text = stringResource(R.string.header_ping),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    if (FetcherId.HOSTNAME in uiState.enabledFetchers) {
                        Text(
                            text = stringResource(R.string.header_hostname),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.0f)
                        )
                    }
                    if (FetcherId.TTL in uiState.enabledFetchers) {
                        Text(
                            text = stringResource(R.string.header_ttl),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.5f)
                        )
                    }
                    if (FetcherId.PORTS in uiState.enabledFetchers) {
                        Text(
                            text = stringResource(R.string.header_ports),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    if (FetcherId.MAC_VENDOR in uiState.enabledFetchers) {
                        Text(
                            text = stringResource(R.string.header_mac),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }

            // Results List / Empty State
            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.searchQuery.isNotBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_search_results, uiState.searchQuery),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(24.dp)
                                .testTag("scanner_empty_state")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.quick_start_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.quick_start_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("scan_results_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = results,
                        key = { _, host -> host.ip }
                    ) { index, host ->
                        ModernHostResultRow(
                            index = index + 1,
                            host = host,
                            enabledFetchers = uiState.enabledFetchers,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedHostForDetails = host
                            }
                        )
                    }
                }
            }
        }
    }

    // Column / Fetcher Selection Dialog
    if (showColumnsDialog) {
        AlertDialog(
            onDismissRequest = { showColumnsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_columns_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dialog_columns_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FetcherId.entries.forEach { fetcherId ->
                        val isEnabled = fetcherId in uiState.enabledFetchers
                        val title = when (fetcherId) {
                            FetcherId.PING -> stringResource(R.string.fetcher_ping)
                            FetcherId.HOSTNAME -> stringResource(R.string.fetcher_hostname)
                            FetcherId.TTL -> stringResource(R.string.fetcher_ttl)
                            FetcherId.MAC_VENDOR -> stringResource(R.string.fetcher_mac)
                            FetcherId.PORTS -> stringResource(R.string.fetcher_ports)
                        }

                        Surface(
                            onClick = { viewModel.toggleFetcher(fetcherId) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isEnabled,
                                    onCheckedChange = { viewModel.toggleFetcher(fetcherId) },
                                    enabled = fetcherId != FetcherId.PING,
                                    modifier = Modifier.testTag("checkbox_fetcher_${fetcherId.name.lowercase()}")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Ports Configuration section if PORTS enabled
                    if (FetcherId.PORTS in uiState.enabledFetchers) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ports_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = uiState.portsInput,
                            onValueChange = { viewModel.onPortsInputChanged(it) },
                            label = { Text(stringResource(R.string.ports_input_label)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_ports_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SuggestionChip(
                                onClick = { viewModel.onPortsInputChanged("22, 80, 443, 8080") },
                                label = { Text("Web & SSH", style = MaterialTheme.typography.labelSmall) }
                            )
                            SuggestionChip(
                                onClick = { viewModel.onPortsInputChanged("21, 22, 80, 443, 3306, 5432, 8080") },
                                label = { Text("Servers", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColumnsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Cancel Active Scan Confirmation Dialog
    if (showCancelScanDialog) {
        AlertDialog(
            onDismissRequest = { showCancelScanDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "Stop Active Scan?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "A network scan is currently in progress. Would you like to stop scanning now?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopScan()
                        showCancelScanDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Scan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelScanDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }

    // Host Details Sheet
    selectedHostForDetails?.let { host ->
        HostDetailSheet(
            host = host,
            onDismiss = { selectedHostForDetails = null },
            onReprobe = { ip ->
                viewModel.reprobeHost(ip) { updated ->
                    selectedHostForDetails = updated
                }
            },
            onSaveComment = { ip, comment ->
                viewModel.setHostComment(ip, comment)
            }
        )
    }

    // Export Dialog (SAF)
    if (showExportDialog) {
        ExportDialog(
            hosts = results,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun SortChip(
    label: String,
    isSelected: Boolean,
    isAscending: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernHostResultRow(
    index: Int,
    host: DiscoveredHost,
    enabledFetchers: Set<FetcherId>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAlive = host.status == HostStatus.ALIVE

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = if (isAlive) 2.dp else 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("host_row_${host.ip.replace('.', '_')}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number (#1, #2, ...)
            Text(
                text = "#$index",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isAlive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .width(36.dp)
                    .testTag("host_index_$index")
            )

            // IP Address (Monospace)
            Text(
                text = host.ip,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isAlive) FontWeight.Bold else FontWeight.Normal,
                color = if (isAlive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.2f)
            )

            // Status Badge
            Box(modifier = Modifier.weight(0.8f)) {
                HostStatusBadge(status = host.status)
            }

            // Latency with color indicator
            if (FetcherId.PING in enabledFetchers) {
                Text(
                    text = host.responseTimeMs?.let { "${it} ms" } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isAlive) FontWeight.Medium else FontWeight.Normal,
                    color = when {
                        host.responseTimeMs == null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        host.responseTimeMs < 30 -> MaterialTheme.colorScheme.primary
                        host.responseTimeMs < 100 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.weight(0.7f)
                )
            }

            // Hostname
            if (FetcherId.HOSTNAME in enabledFetchers) {
                Row(
                    modifier = Modifier.weight(1.0f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!host.hostname.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = host.hostname ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!host.hostname.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // TTL
            if (FetcherId.TTL in enabledFetchers) {
                Text(
                    text = host.ttl?.toString() ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.5f)
                )
            }

            // Open Ports
            if (FetcherId.PORTS in enabledFetchers) {
                val portsText = if (host.openPorts.isNotEmpty()) {
                    host.openPorts.joinToString(",")
                } else if (isAlive) {
                    "None"
                } else {
                    "—"
                }
                Text(
                    text = portsText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (host.openPorts.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.9f)
                )
            }

            // MAC & Vendor
            if (FetcherId.MAC_VENDOR in enabledFetchers) {
                val macDisplay = when {
                    host.vendor != null -> host.vendor
                    host.macAddress != null -> host.macAddress
                    else -> "—"
                }
                Text(
                    text = macDisplay ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

