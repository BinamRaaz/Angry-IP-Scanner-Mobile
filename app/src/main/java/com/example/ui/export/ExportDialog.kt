package com.example.ui.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.export.ExportConfig
import com.example.core.export.ExportFormat
import com.example.core.export.ScanExportManager
import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExportDialog(
    hosts: List<DiscoveredHost>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var aliveOnly by remember { mutableStateOf(false) }
    var includeHostname by remember { mutableStateOf(true) }
    var includePing by remember { mutableStateOf(true) }
    var includeTtl by remember { mutableStateOf(true) }
    var includeMacVendor by remember { mutableStateOf(true) }
    var includePorts by remember { mutableStateOf(true) }
    var includeComments by remember { mutableStateOf(true) }

    val config = remember(selectedFormat, aliveOnly, includeHostname, includePing, includeTtl, includeMacVendor, includePorts, includeComments) {
        ExportConfig(
            format = selectedFormat,
            aliveOnly = aliveOnly,
            includeHostname = includeHostname,
            includePing = includePing,
            includeTtl = includeTtl,
            includeMacVendor = includeMacVendor,
            includePorts = includePorts,
            includeComments = includeComments
        )
    }

    val totalToExport = if (aliveOnly) hosts.count { it.status == HostStatus.ALIVE } else hosts.size

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = writeExportToUri(context, uri, hosts, config)
                if (success) {
                    Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_LONG).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, context.getString(R.string.export_error, "Write error"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.export_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Format Selector Chips
                Text(
                    text = stringResource(R.string.export_format_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("export_chip_${format.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider()

                // Alive Hosts Only Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.export_alive_only),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$totalToExport hosts will be exported",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Switch(
                        checked = aliveOnly,
                        onCheckedChange = { aliveOnly = it },
                        modifier = Modifier.testTag("export_alive_only_switch")
                    )
                }

                HorizontalDivider()

                // Column Toggles
                Text(
                    text = stringResource(R.string.export_columns_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExportCheckboxRow("Hostname / Reverse DNS", includeHostname) { includeHostname = it }
                ExportCheckboxRow("Ping / Response Latency", includePing) { includePing = it }
                ExportCheckboxRow("TTL (Packet Time-to-Live)", includeTtl) { includeTtl = it }
                ExportCheckboxRow("MAC Address & Manufacturer", includeMacVendor) { includeMacVendor = it }
                ExportCheckboxRow("Open Ports & Services", includePorts) { includePorts = it }
                ExportCheckboxRow("User Notes / Comments", includeComments) { includeComments = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val defaultName = ScanExportManager.generateDefaultFileName(selectedFormat)
                    createDocLauncher.launch(defaultName)
                },
                modifier = Modifier.testTag("start_export_saf_button"),
                enabled = totalToExport > 0
            ) {
                Text(stringResource(R.string.btn_export_file))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ExportCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private suspend fun writeExportToUri(
    context: Context,
    uri: Uri,
    hosts: List<DiscoveredHost>,
    config: ExportConfig
): Boolean = withContext(Dispatchers.IO) {
    try {
        val content = ScanExportManager.generateContent(hosts, config)
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(content.toByteArray(Charsets.UTF_8))
            os.flush()
        }
        true
    } catch (_: Exception) {
        false
    }
}
