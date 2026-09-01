package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransferDirection
import com.example.data.TransferEntity
import com.example.data.TransferRepository
import com.example.data.TransferStatus
import com.example.data.UserPreferencesRepository
import com.example.protocol.ContactPayload
import com.example.protocol.QRProtocolEngine
import com.example.protocol.TransferFileItem
import com.example.protocol.TransferPayloadType
import com.example.transfer.SenderEngine
import com.example.ui.components.AirGapBadge
import com.example.ui.components.EncryptionBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassProgressBar
import com.example.ui.components.GlassSegmentedControl
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurpleSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
fun SenderScreen(
    initialType: TransferPayloadType?,
    transferRepository: TransferRepository,
    preferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val preferences by preferencesRepository.preferencesFlow.collectAsState()

    var selectedType by remember {
        mutableStateOf(initialType ?: TransferPayloadType.TEXT)
    }

    // Input States
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("https://") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }
    var contactNote by remember { mutableStateOf("") }

    var selectedFileUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFileItems by remember { mutableStateOf<List<TransferFileItem>>(emptyList()) }
    var isProcessingPayload by remember { mutableStateOf(false) }

    val senderEngine = remember { SenderEngine(scope) }
    val senderState by senderEngine.uiState.collectAsState()

    var showMissingFramesDialog by remember { mutableStateOf(false) }
    var missingFramesInput by remember { mutableStateOf("") }

    // File pickers
    val singleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val item = resolveFileMetadata(context, uri)
            selectedFileUris = listOf(uri)
            selectedFileItems = listOf(item)
        }
    }

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val items = uris.map { resolveFileMetadata(context, it) }
            selectedFileUris = uris
            selectedFileItems = items
        }
    }

    // Handle Keep Screen Awake & Brightness
    DisposableEffect(senderState.isReady, preferences.keepScreenAwake, preferences.maxBrightness) {
        val window = (context as? Activity)?.window
        if (senderState.isReady) {
            if (preferences.keepScreenAwake) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            if (preferences.maxBrightness) {
                val params = window?.attributes
                params?.screenBrightness = 1.0f
                window?.attributes = params
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window?.attributes
            params?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = params
            senderEngine.release()
        }
    }

    if (senderState.isReady) {
        // ACTIVE BROADCAST SCREEN
        ActiveBroadcastView(
            senderState = senderState,
            senderEngine = senderEngine,
            onClose = {
                senderEngine.release()
                onBack()
            },
            onOpenMissingFramesFilter = { showMissingFramesDialog = true }
        )

        if (showMissingFramesDialog) {
            AlertDialog(
                onDismissRequest = { showMissingFramesDialog = false },
                title = { Text("Replay Missing Frames") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter the missing frame numbers requested by receiver (e.g. 3, 14, 25):",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = missingFramesInput,
                            onValueChange = { missingFramesInput = it },
                            placeholder = { Text("3, 7, 12") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val indices = missingFramesInput
                                .split(",", " ", "\n")
                                .mapNotNull { it.trim().toIntOrNull() }
                                .toSet()
                            senderEngine.filterMissingFrames(indices)
                            showMissingFramesDialog = false
                        }
                    ) {
                        Text("Replay Frames")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            senderEngine.clearMissingFramesFilter()
                            showMissingFramesDialog = false
                        }
                    ) {
                        Text("Reset All")
                    }
                }
            )
        }
    } else {
        // PREPARE / COMPOSE SCREEN
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
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
                        text = "Send Data",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    AirGapBadge()
                }
            }

            // Payload Type Selector
            item {
                val types = listOf("Text", "Link", "Contact", "File", "Multi-Files")
                val currentIdx = when (selectedType) {
                    TransferPayloadType.TEXT -> 0
                    TransferPayloadType.URL -> 1
                    TransferPayloadType.CONTACT -> 2
                    TransferPayloadType.FILE -> 3
                    TransferPayloadType.MULTI_FILE -> 4
                }

                GlassSegmentedControl(
                    items = types,
                    selectedIndex = currentIdx,
                    onItemSelected = { index ->
                        selectedType = when (index) {
                            0 -> TransferPayloadType.TEXT
                            1 -> TransferPayloadType.URL
                            2 -> TransferPayloadType.CONTACT
                            3 -> TransferPayloadType.FILE
                            else -> TransferPayloadType.MULTI_FILE
                        }
                    }
                )
            }

            // Type-Specific Form Inputs
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedType) {
                            TransferPayloadType.TEXT -> {
                                Text(
                                    text = "Text Note",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = { Text("Type message, secret keys, or offline notes...") },
                                    minLines = 4,
                                    maxLines = 8,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sender_text_input"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Text(
                                    text = "${textInput.length} characters",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TransferPayloadType.URL -> {
                                Text(
                                    text = "Web Link / URL",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    placeholder = { Text("https://example.com") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sender_url_input"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }

                            TransferPayloadType.CONTACT -> {
                                Text(
                                    text = "Contact Card (vCard)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = contactName,
                                    onValueChange = { contactName = it },
                                    label = { Text("Full Name *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sender_contact_name"),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = contactPhone,
                                    onValueChange = { contactPhone = it },
                                    label = { Text("Phone Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = contactEmail,
                                    onValueChange = { contactEmail = it },
                                    label = { Text("Email Address") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = contactOrg,
                                    onValueChange = { contactOrg = it },
                                    label = { Text("Organization / Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            TransferPayloadType.FILE -> {
                                Text(
                                    text = "Single File Transfer",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (selectedFileItems.isNotEmpty()) {
                                    val file = selectedFileItems.first()
                                    FileItemRow(file = file, onRemove = {
                                        selectedFileUris = emptyList()
                                        selectedFileItems = emptyList()
                                    })
                                } else {
                                    GlassButton(
                                        text = "Select Document, Photo, or File",
                                        icon = Icons.Default.Add,
                                        isPrimary = false,
                                        onClick = { singleFilePicker.launch("*/*") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            TransferPayloadType.MULTI_FILE -> {
                                Text(
                                    text = "Multiple Files Package",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (selectedFileItems.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        selectedFileItems.forEachIndexed { index, file ->
                                            FileItemRow(file = file, onRemove = {
                                                selectedFileItems = selectedFileItems.filterIndexed { i, _ -> i != index }
                                                selectedFileUris = selectedFileUris.filterIndexed { i, _ -> i != index }
                                            })
                                        }
                                        GlassButton(
                                            text = "Add More Files",
                                            icon = Icons.Default.Add,
                                            isPrimary = false,
                                            onClick = { multiFilePicker.launch("*/*") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    GlassButton(
                                        text = "Select Multiple Files",
                                        icon = Icons.Default.Add,
                                        isPrimary = false,
                                        onClick = { multiFilePicker.launch("*/*") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transfer Security Info Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EncryptionBadge()
                    Text(
                        text = "Chunk Size: ${preferences.chunkSizeBytes}B",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Start Transfer Button
            item {
                val canStart = when (selectedType) {
                    TransferPayloadType.TEXT -> textInput.isNotBlank()
                    TransferPayloadType.URL -> urlInput.isNotBlank() && urlInput.length > 8
                    TransferPayloadType.CONTACT -> contactName.isNotBlank()
                    TransferPayloadType.FILE, TransferPayloadType.MULTI_FILE -> selectedFileItems.isNotEmpty()
                }

                if (isProcessingPayload) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                } else {
                    GlassButton(
                        text = "Generate QR Broadcast",
                        icon = Icons.Default.PlayArrow,
                        enabled = canStart,
                        onClick = {
                            isProcessingPayload = true
                            scope.launch(Dispatchers.Default) {
                                processAndStartTransfer(
                                    context = context,
                                    selectedType = selectedType,
                                    textInput = textInput,
                                    urlInput = urlInput,
                                    contact = ContactPayload(
                                        name = contactName,
                                        phone = contactPhone,
                                        email = contactEmail,
                                        organization = contactOrg,
                                        note = contactNote
                                    ),
                                    fileUris = selectedFileUris,
                                    fileItems = selectedFileItems,
                                    chunkSize = preferences.chunkSizeBytes,
                                    encrypt = preferences.encryptionEnabled,
                                    compress = preferences.compressionEnabled,
                                    senderEngine = senderEngine,
                                    transferRepository = transferRepository,
                                    speedMs = preferences.frameSpeedMs
                                )
                                withContext(Dispatchers.Main) {
                                    isProcessingPayload = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_transfer_button")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ActiveBroadcastView(
    senderState: com.example.transfer.SenderUiState,
    senderEngine: SenderEngine,
    onClose: () -> Unit,
    onOpenMissingFramesFilter: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val manifest = senderState.manifest

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.Close,
                    onClick = onClose,
                    contentDescription = "Cancel Transfer"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Broadcasting QR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ID: ${manifest?.transferId ?: ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElectricCyan
                    )
                }

                EncryptionBadge()
            }
        }

        // Main High-Contrast QR Code Card
        item {
            GlassCard(
                modifier = Modifier
                    .size(310.dp),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = Color.White, // Pure crisp white surface for high camera scanning contrast
                elevation = 16.dp,
                testTag = "sender_qr_canvas"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = senderState.currentBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR Code Frame",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = AppleBlue)
                    }
                }
            }
        }

        // Frame Status & Liquid Progress
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isFiltering = senderState.targetMissingFrames.isNotEmpty()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFiltering) {
                            "Replaying Frame ${senderState.currentFrameIndex} (Targeted Loop)"
                        } else {
                            "Frame ${senderState.currentFrameIndex} of ${senderState.totalFrames}"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFiltering) PurpleSecurity else MaterialTheme.colorScheme.onSurface
                    )

                    val progressFloat = senderState.currentFrameIndex.toFloat() / senderState.totalFrames.coerceAtLeast(1)
                    Text(
                        text = "${(progressFloat * 100).toInt()}% • Loop #${senderState.completedLoops + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlassProgressBar(
                    progress = senderState.currentFrameIndex.toFloat() / senderState.totalFrames.coerceAtLeast(1),
                    height = 8.dp
                )
            }
        }

        // Player Controls (Prev, Play/Pause, Next)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.FastRewind,
                    onClick = { senderEngine.stepPrev() },
                    contentDescription = "Previous Frame"
                )

                Spacer(modifier = Modifier.width(20.dp))

                // Big Play / Pause Pill
                GlassButton(
                    text = if (senderState.isPlaying) "Pause" else "Resume",
                    icon = if (senderState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = { senderEngine.togglePlayPause() },
                    modifier = Modifier.width(150.dp),
                    testTag = "sender_play_pause_button"
                )

                Spacer(modifier = Modifier.width(20.dp))

                GlassIconButton(
                    icon = Icons.Default.FastForward,
                    onClick = { senderEngine.stepNext() },
                    contentDescription = "Next Frame"
                )
            }
        }

        // Speed & Missing Frames Actions
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame Speed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SpeedChip(label = "Fast (100ms)", selected = senderState.speedMs <= 110) {
                                senderEngine.setSpeedMs(100)
                            }
                            SpeedChip(label = "Normal (160ms)", selected = senderState.speedMs in 111..199) {
                                senderEngine.setSpeedMs(160)
                            }
                            SpeedChip(label = "Safe (250ms)", selected = senderState.speedMs >= 200) {
                                senderEngine.setSpeedMs(250)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Missing Frames",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        GlassButton(
                            text = if (senderState.targetMissingFrames.isNotEmpty()) "Filtering (${senderState.targetMissingFrames.size})" else "Target Replay",
                            icon = Icons.Default.FilterAlt,
                            isPrimary = false,
                            onClick = onOpenMissingFramesFilter
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SpeedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) AppleBlue else if (isDark) Color(0x22FFFFFF) else Color(0x10000000)
            )
            .border(
                1.dp,
                if (selected) ElectricCyan else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FileItemRow(
    file: TransferFileItem,
    onRemove: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = formatFileSize(file.fileSize),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassIconButton(
            icon = Icons.Default.Close,
            onClick = onRemove,
            size = 32.dp,
            contentDescription = "Remove file"
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun resolveFileMetadata(context: Context, uri: Uri): TransferFileItem {
    var name = "file_${System.currentTimeMillis()}"
    var size = 0L
    var mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }

    return TransferFileItem(
        fileName = name,
        fileSize = size,
        mimeType = mime
    )
}

private suspend fun processAndStartTransfer(
    context: Context,
    selectedType: TransferPayloadType,
    textInput: String,
    urlInput: String,
    contact: ContactPayload,
    fileUris: List<Uri>,
    fileItems: List<TransferFileItem>,
    chunkSize: Int,
    encrypt: Boolean,
    compress: Boolean,
    senderEngine: SenderEngine,
    transferRepository: TransferRepository,
    speedMs: Int
) {
    val payloadBytes: ByteArray
    val title: String
    val filesList: List<TransferFileItem>

    when (selectedType) {
        TransferPayloadType.TEXT -> {
            title = textInput.take(30).ifBlank { "Text Note" }
            payloadBytes = textInput.toByteArray(Charsets.UTF_8)
            filesList = emptyList()
        }
        TransferPayloadType.URL -> {
            title = urlInput
            payloadBytes = urlInput.toByteArray(Charsets.UTF_8)
            filesList = emptyList()
        }
        TransferPayloadType.CONTACT -> {
            title = contact.name.ifBlank { "Contact Card" }
            val vcard = contact.toVCard()
            payloadBytes = vcard.toByteArray(Charsets.UTF_8)
            filesList = emptyList()
        }
        TransferPayloadType.FILE -> {
            val uri = fileUris.firstOrNull() ?: return
            val fileMeta = fileItems.firstOrNull() ?: resolveFileMetadata(context, uri)
            title = fileMeta.fileName
            payloadBytes = readBytesFromUri(context, uri)
            filesList = listOf(fileMeta.copy(fileSize = payloadBytes.size.toLong()))
        }
        TransferPayloadType.MULTI_FILE -> {
            title = "${fileItems.size} Files Package"
            val byteOut = ByteArrayOutputStream()
            val updatedList = mutableListOf<TransferFileItem>()
            fileUris.forEachIndexed { idx, uri ->
                val meta = fileItems.getOrNull(idx) ?: resolveFileMetadata(context, uri)
                val bytes = readBytesFromUri(context, uri)
                byteOut.write(bytes)
                updatedList.add(meta.copy(fileSize = bytes.size.toLong()))
            }
            payloadBytes = byteOut.toByteArray()
            filesList = updatedList
        }
    }

    val (manifest, frames) = QRProtocolEngine.createTransferFrames(
        type = selectedType,
        title = title,
        payloadBytes = payloadBytes,
        files = filesList,
        chunkSizeBytes = chunkSize,
        encrypt = encrypt,
        compress = compress
    )

    // Save to Room DB as Sent Transfer
    val entity = TransferEntity(
        transferId = manifest.transferId,
        direction = TransferDirection.SENT,
        payloadType = selectedType.code,
        title = title,
        subtitle = "${frames.size} frames (${formatFileSize(payloadBytes.size.toLong())})",
        sizeBytes = payloadBytes.size.toLong(),
        frameCount = frames.size,
        status = TransferStatus.COMPLETED,
        sha256Checksum = manifest.overallSha256,
        isEncrypted = encrypt,
        detailsJson = if (selectedType == TransferPayloadType.TEXT || selectedType == TransferPayloadType.URL) String(payloadBytes, Charsets.UTF_8) else "",
        timestamp = System.currentTimeMillis()
    )
    transferRepository.saveTransfer(entity)

    withContext(Dispatchers.Main) {
        senderEngine.setupTransfer(manifest, frames, initialSpeedMs = speedMs)
    }
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray {
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.readBytes()
    } ?: ByteArray(0)
}
