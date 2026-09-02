package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import com.example.util.MediaCategory
import com.example.util.MediaExportHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.TransferDirection
import com.example.data.TransferEntity
import com.example.data.TransferRepository
import com.example.ui.components.AirGapBadge
import com.example.ui.components.EncryptionBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassSegmentedControl
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transferRepository: TransferRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDark.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedTransferForDetail by remember { mutableStateOf<TransferEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val allTransfers by transferRepository.allTransfers.collectAsState(initial = emptyList())
    val sentTransfers by transferRepository.sentTransfers.collectAsState(initial = emptyList())
    val receivedTransfers by transferRepository.receivedTransfers.collectAsState(initial = emptyList())

    val displayedList = when (selectedTab) {
        1 -> receivedTransfers
        2 -> sentTransfers
        else -> allTransfers
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back"
                )

                Text(
                    text = "Transfer History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (displayedList.isNotEmpty()) {
                    GlassIconButton(
                        icon = Icons.Default.DeleteSweep,
                        onClick = { showClearConfirmDialog = true },
                        contentDescription = "Clear all history",
                        tint = Color(0xFFFF453A)
                    )
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        }

        // Segmented Tab (All, Received, Sent)
        item {
            GlassSegmentedControl(
                items = listOf("All (${allTransfers.size})", "Received (${receivedTransfers.size})", "Sent (${sentTransfers.size})"),
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        }

        if (displayedList.isEmpty()) {
            item {
                EmptyHistoryState(
                    tabName = when (selectedTab) {
                        1 -> "received"
                        2 -> "sent"
                        else -> "completed"
                    }
                )
            }
        } else {
            items(displayedList, key = { it.id }) { transfer ->
                HistoryItemCard(
                    transfer = transfer,
                    onClick = { selectedTransferForDetail = transfer }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Detail Bottom Sheet
    if (selectedTransferForDetail != null) {
        val transfer = selectedTransferForDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedTransferForDetail = null },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = transfer.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Transfer ID: ${transfer.transferId}",
                            fontSize = 12.sp,
                            color = ElectricCyan
                        )
                    }
                    EncryptionBadge()
                }

                // Metadata Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Direction", value = if (transfer.direction == TransferDirection.SENT) "Sent" else "Received")
                    DetailRow(label = "Type", value = transfer.payloadType)
                    DetailRow(label = "Size", value = formatBytes(transfer.sizeBytes))
                    DetailRow(label = "QR Frames", value = "${transfer.frameCount} frames")
                    DetailRow(label = "Date & Time", value = formatTimestamp(transfer.timestamp))
                    if (transfer.sha256Checksum.isNotBlank()) {
                        DetailRow(label = "SHA-256", value = transfer.sha256Checksum.take(16) + "...")
                    }
                }

                // Text detail if available
                if (transfer.detailsJson.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x0A000000))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = transfer.detailsJson,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Action Buttons
                if (transfer.filePath != null && File(transfer.filePath).exists()) {
                    val file = File(transfer.filePath)
                    val rawName = file.name.substringAfter('_', file.name)
                    val category = MediaExportHelper.determineCategory(rawName, "")
                    val saveBtnText = when (category) {
                        MediaCategory.IMAGE, MediaCategory.VIDEO -> "Save to Gallery"
                        MediaCategory.AUDIO -> "Save to Music"
                        MediaCategory.DOCUMENT -> "Save to Downloads"
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                text = saveBtnText,
                                icon = Icons.Default.Download,
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val res = MediaExportHelper.saveFileToDevice(
                                        context = context,
                                        sourceFile = file,
                                        originalFileName = rawName,
                                        mimeType = ""
                                    )
                                    Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                }
                            )

                            GlassIconButton(
                                icon = Icons.Default.OpenInNew,
                                contentDescription = "Open file",
                                tint = ElectricCyan,
                                onClick = {
                                    MediaExportHelper.openInSystemViewer(context, file, "")
                                }
                            )

                            GlassIconButton(
                                icon = Icons.Default.Share,
                                contentDescription = "Share file",
                                onClick = {
                                    shareExistingFile(context, file)
                                }
                            )

                            GlassIconButton(
                                icon = Icons.Default.Delete,
                                tint = Color(0xFFFF453A),
                                contentDescription = "Delete record",
                                onClick = {
                                    scope.launch {
                                        transferRepository.deleteTransfer(transfer.id)
                                        selectedTransferForDetail = null
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (transfer.detailsJson.isNotBlank()) {
                            GlassButton(
                                text = "Copy Text",
                                icon = Icons.Default.ContentCopy,
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("DropQR", transfer.detailsJson))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        GlassIconButton(
                            icon = Icons.Default.Delete,
                            tint = Color(0xFFFF453A),
                            contentDescription = "Delete record",
                            onClick = {
                                scope.launch {
                                    transferRepository.deleteTransfer(transfer.id)
                                    selectedTransferForDetail = null
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Clear History Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Transfer History") },
            text = { Text("Are you sure you want to permanently delete all local transfer logs? Transferred files stored in your storage will remain untouched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            transferRepository.clearAllHistory()
                            showClearConfirmDialog = false
                        }
                    }
                ) {
                    Text("Clear All", color = Color(0xFFFF453A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(
    transfer: TransferEntity,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isSent = transfer.direction == TransferDirection.SENT

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        testTag = "history_item_${transfer.id}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSent) AppleBlue.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSent) Icons.Default.CallMade else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = if (isSent) AppleBlue else EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transfer.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (transfer.subtitle.isNotBlank()) transfer.subtitle else formatBytes(transfer.sizeBytes),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestampShort(transfer.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${transfer.frameCount} frames",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ElectricCyan
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(tabName: String) {
    val isDark = LocalIsDark.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "No $tabName transfers yet",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Files and notes you transfer offline via QR will appear here.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTimestampShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun shareExistingFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    } catch (_: Exception) {
        Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show()
    }
}
