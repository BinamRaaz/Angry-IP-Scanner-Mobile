package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.models.DiscoveredHost
import com.example.core.utilities.PortUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HostDetailSheet(
    host: DiscoveredHost,
    onDismiss: () -> Unit,
    onReprobe: (String) -> Unit,
    onSaveComment: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var isReprobing by remember { mutableStateOf(false) }
    var currentHost by remember(host) { mutableStateOf(host) }
    var commentInput by remember(host.comment) { mutableStateOf(host.comment ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("host_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentHost.ip,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        currentHost.hostname?.let { hn ->
                            Text(
                                text = hn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HostStatusBadge(status = currentHost.status)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Quick Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy IP Button
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, "IP Address", currentHost.ip)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_copy_ip_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.action_copy_ip), style = MaterialTheme.typography.labelMedium)
                }

                // Re-probe Button
                OutlinedButton(
                    onClick = {
                        isReprobing = true
                        onReprobe(currentHost.ip)
                    },
                    enabled = !isReprobing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_reprobe_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.action_reprobe), style = MaterialTheme.typography.labelMedium)
                }

                // Share Button
                OutlinedButton(
                    onClick = {
                        shareHostDetails(context, currentHost)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_share_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.action_share), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Service Launchers (Web, SSH, etc.)
            val hasWeb = 80 in currentHost.openPorts
            val hasWebSsl = 443 in currentHost.openPorts
            val hasSsh = 22 in currentHost.openPorts

            if (hasWeb || hasWebSsl || hasSsh) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_services),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasWeb) {
                                FilledTonalButton(
                                    onClick = {
                                        openUri(context, "http://${currentHost.ip}")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("HTTP:80", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (hasWebSsl) {
                                FilledTonalButton(
                                    onClick = {
                                        openUri(context, "https://${currentHost.ip}")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("HTTPS:443", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (hasSsh) {
                                FilledTonalButton(
                                    onClick = {
                                        openUri(context, "ssh://${currentHost.ip}:22")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SSH:22", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Host Properties Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PropertyRow(
                        label = "IP Address",
                        value = currentHost.ip,
                        onCopy = { copyToClipboard(context, "IP Address", currentHost.ip) }
                    )

                    currentHost.hostname?.let { hn ->
                        PropertyRow(
                            label = "Hostname",
                            value = hn,
                            onCopy = { copyToClipboard(context, "Hostname", hn) }
                        )
                    }

                    currentHost.responseTimeMs?.let { ms ->
                        PropertyRow(
                            label = "Ping / Latency",
                            value = "$ms ms"
                        )
                    }

                    currentHost.ttl?.let { ttl ->
                        PropertyRow(
                            label = "TTL",
                            value = "$ttl"
                        )
                    }

                    currentHost.macAddress?.let { mac ->
                        PropertyRow(
                            label = "MAC Address",
                            value = mac,
                            onCopy = { copyToClipboard(context, "MAC Address", mac) }
                        )
                    }

                    currentHost.vendor?.let { vendor ->
                        PropertyRow(
                            label = "Manufacturer",
                            value = vendor
                        )
                    }

                    if (currentHost.openPorts.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Discovered Open Ports (${currentHost.openPorts.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentHost.openPorts.forEach { port ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.testTag("detail_port_$port")
                                ) {
                                    Text(
                                        text = "$port / ${PortUtils.getServiceName(port)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // User Notes / Comment Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_host_comment),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text(stringResource(R.string.hint_host_comment)) },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("host_comment_input")
                    )

                    Button(
                        onClick = {
                            onSaveComment(currentHost.ip, commentInput)
                            currentHost = currentHost.copy(comment = commentInput.ifBlank { null })
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_host_comment_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.btn_save_note))
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            if (onCopy != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareHostDetails(context: Context, host: DiscoveredHost) {
    val details = buildString {
        appendLine("Host: ${host.ip}")
        host.hostname?.let { appendLine("Hostname: $it") }
        appendLine("Status: ${host.status.name}")
        host.responseTimeMs?.let { appendLine("Latency: $it ms") }
        host.ttl?.let { appendLine("TTL: $it") }
        host.macAddress?.let { appendLine("MAC: $it") }
        host.vendor?.let { appendLine("Vendor: $it") }
        if (host.openPorts.isNotEmpty()) {
            appendLine("Open Ports: ${host.openPorts.joinToString(", ") { "$it (${PortUtils.getServiceName(it)})" }}")
        }
        host.comment?.let { appendLine("Notes: $it") }
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, details)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Host Details"))
}

private fun openUri(context: Context, uriString: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No app available to handle $uriString", Toast.LENGTH_SHORT).show()
    }
}
