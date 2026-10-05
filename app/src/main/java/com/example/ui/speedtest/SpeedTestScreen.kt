package com.example.ui.speedtest

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.speedtest.SpeedSample
import com.example.core.speedtest.SpeedTestPhase
import com.example.core.speedtest.SpeedTestProgress
import com.example.core.speedtest.SpeedTestResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedTestScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeedTestViewModel = viewModel()
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastCompletedResult.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("speed_test_screen")
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("speed_test_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_button)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = stringResource(R.string.speed_test_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = progress.serverName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (progress.phase == SpeedTestPhase.COMPLETED || lastResult != null) {
                IconButton(
                    onClick = {
                        val result = lastResult ?: return@IconButton
                        val shareText = buildString {
                            appendLine("🌐 Internet Speed Test Results")
                            appendLine("Ping: ${String.format(Locale.US, "%.1f", result.pingMs)} ms (Jitter: ${String.format(Locale.US, "%.1f", result.jitterMs)} ms)")
                            appendLine("Download: ${String.format(Locale.US, "%.2f", result.downloadMbps)} Mbps")
                            appendLine("Upload: ${String.format(Locale.US, "%.2f", result.uploadMbps)} Mbps")
                            appendLine("Server: ${result.serverName}")
                            appendLine("Tested via Angry IP Scanner Mobile")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Speed Test Result"))
                    },
                    modifier = Modifier.testTag("btn_share_speed_test")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.speed_test_btn_share),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Speedometer Gauge Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("speedometer_gauge_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Determine current displayed speed for the gauge
                val displaySpeed = when (progress.phase) {
                    SpeedTestPhase.DOWNLOAD -> progress.currentDownloadMbps
                    SpeedTestPhase.UPLOAD -> progress.currentUploadMbps
                    SpeedTestPhase.COMPLETED -> progress.currentDownloadMbps
                    else -> 0.0
                }

                SpeedometerGauge(
                    currentSpeedMbps = displaySpeed,
                    phase = progress.phase,
                    modifier = Modifier
                        .size(220.dp)
                        .testTag("speed_test_gauge")
                )

                // Phase Status Chip / Progress indicator
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = progress.phase.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (progress.isRunning) {
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("speed_test_progress_bar"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (progress.isRunning) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.cancelSpeedTest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("btn_cancel_speed_test")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.speed_test_btn_cancel), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.startSpeedTest()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(48.dp)
                                .testTag("btn_start_speed_test")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (progress.phase == SpeedTestPhase.COMPLETED) "Test Again" else stringResource(R.string.speed_test_btn_start),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }

        // Live Metrics Row (Ping, Jitter, Download, Upload)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ping & Jitter Card
            SpeedMetricCard(
                title = "Latency",
                value = progress.avgPingMs?.let { String.format(Locale.US, "%.0f ms", it) } ?: "—",
                subtext = progress.jitterMs?.let { "Jitter: ${String.format(Locale.US, "%.1f", it)} ms" } ?: "Jitter: —",
                icon = Icons.Default.Timer,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_card_ping")
            )

            // Download Card
            SpeedMetricCard(
                title = "Download",
                value = if (progress.peakDownloadMbps > 0 || progress.currentDownloadMbps > 0) {
                    String.format(Locale.US, "%.1f", maxOf(progress.peakDownloadMbps, progress.currentDownloadMbps))
                } else "—",
                subtext = "Mbps",
                icon = Icons.Default.ArrowDownward,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_card_download")
            )

            // Upload Card
            SpeedMetricCard(
                title = "Upload",
                value = if (progress.peakUploadMbps > 0 || progress.currentUploadMbps > 0) {
                    String.format(Locale.US, "%.1f", maxOf(progress.peakUploadMbps, progress.currentUploadMbps))
                } else "—",
                subtext = "Mbps",
                icon = Icons.Default.ArrowUpward,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("speed_card_upload")
            )
        }

        // Live Sparkline Chart during active test
        val activeSamples = if (progress.phase == SpeedTestPhase.DOWNLOAD) {
            progress.downloadSamples
        } else if (progress.phase == SpeedTestPhase.UPLOAD) {
            progress.uploadSamples
        } else emptyList()

        if (activeSamples.size >= 2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("speed_sparkline_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (progress.phase == SpeedTestPhase.DOWNLOAD) "Download Bandwidth Graph" else "Upload Bandwidth Graph",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SpeedSparkline(
                        samples = activeSamples,
                        color = if (progress.phase == SpeedTestPhase.DOWNLOAD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // History Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.speed_test_history_title) + if (history.isNotEmpty()) " (${history.size})" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.testTag("btn_clear_speed_history")
                ) {
                    Text("Clear All", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Speed Test History List
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.speed_test_history_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("speed_history_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(history, key = { _, item -> item.id }) { index, item ->
                    SpeedTestHistoryRow(
                        index = index + 1,
                        result = item,
                        onDelete = { viewModel.deleteHistoryItem(item.id) }
                    )
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(text = "Clear Speed Test History?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "This will delete all past recorded speed test measurements.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SpeedometerGauge(
    currentSpeedMbps: Double,
    phase: SpeedTestPhase,
    modifier: Modifier = Modifier
) {
    // Map speed to gauge angle (0 to 180 degrees)
    // 0 Mbps = 0 deg, 100 Mbps = ~140 deg, 500+ Mbps = 180 deg
    val normalizedFraction = (currentSpeedMbps / 150.0).coerceIn(0.0, 1.0).toFloat()
    val animatedAngle by animateFloatAsState(
        targetValue = normalizedFraction,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "gauge_angle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.width - strokeWidth * 2
            val topLeft = Offset(strokeWidth, strokeWidth)

            // Background Arc (180 degrees sweep from 180 to 360)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Colored Sweep Arc
            val sweep = 180f * animatedAngle
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    ),
                    startAngle = 180f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Needle indicator
            val center = Offset(size.width / 2f, size.height / 2f + 16.dp.toPx())
            val needleAngleRad = (180f + (180f * animatedAngle)) * (PI / 180f)
            val needleLength = (arcSize / 2f) - 10.dp.toPx()
            val needleEnd = Offset(
                x = center.x + (needleLength * cos(needleAngleRad)).toFloat(),
                y = center.y + (needleLength * sin(needleAngleRad)).toFloat()
            )

            drawLine(
                color = primaryColor,
                start = center,
                end = needleEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = center)
        }

        // Live Speed Center Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", currentSpeedMbps),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mbps",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SpeedMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SpeedSparkline(
    samples: List<SpeedSample>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (samples.size < 2) return

    val maxSpeed = (samples.maxOfOrNull { it.speedMbps } ?: 1.0).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val path = Path()
        val stepX = size.width / (samples.size - 1)

        samples.forEachIndexed { index, sample ->
            val x = index * stepX
            val y = size.height - ((sample.speedMbps / maxSpeed).toFloat() * size.height).coerceIn(0f, size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SpeedTestHistoryRow(
    index: Int,
    result: SpeedTestResult,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(result.timestamp) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(result.timestamp))
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("speed_history_row_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(32.dp)
                )

                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↓ ${String.format(Locale.US, "%.1f", result.downloadMbps)} Mbps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "↑ ${String.format(Locale.US, "%.1f", result.uploadMbps)} Mbps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f", result.pingMs)}ms",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
