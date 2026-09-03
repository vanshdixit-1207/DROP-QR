package com.example.ui.screens

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillBadge
import com.example.ui.components.ChunkMatrixGrid
import com.example.ui.components.EncryptionBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassProgressBar
import com.example.ui.components.GlassSegmentedControl
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoEmeraldDark
import com.example.ui.theme.BentoLavender
import com.example.ui.theme.BentoLavenderDark
import com.example.ui.theme.BentoPrimaryBlue
import com.example.ui.theme.BentoPrimaryBlueDark
import com.example.ui.theme.BentoSky
import com.example.ui.theme.BentoSkyDark
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.BentoSurfaceLight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity
import com.example.util.AudioPlayerHelper
import com.example.util.AudioRecorderHelper
import com.example.util.PaperBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val isDark = LocalIsDark.current

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
    var cryptoInput by remember { mutableStateOf("") }

    // Password / PIN Protection State
    var isPasswordProtectionEnabled by remember { mutableStateOf(false) }
    var customPassword by remember { mutableStateOf("") }
    var passwordHint by remember { mutableStateOf("") }

    // Time-Lock State
    var isTimeLockEnabled by remember { mutableStateOf(false) }
    var timeLockDurationIndex by remember { mutableStateOf(0) } // 0: 15m, 1: 1h, 2: 6h, 3: 24h, 4: 3d, 5: 7d

    // Voice Note State
    val audioRecorderHelper = remember { AudioRecorderHelper(context) }
    val audioPlayerHelper = remember { AudioPlayerHelper(context) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var isPlayingRecordedAudio by remember { mutableStateOf(false) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileMeta by remember { mutableStateOf<TransferFileItem?>(null) }
    var isProcessingPayload by remember { mutableStateOf(false) }

    val senderEngine = remember { SenderEngine(scope) }
    val senderState by senderEngine.uiState.collectAsState()

    var showMissingFramesDialog by remember { mutableStateOf(false) }
    var missingFramesInput by remember { mutableStateOf("") }
    var isAmbientFullscreen by remember { mutableStateOf(false) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val item = resolveFileMetadata(context, uri)
            selectedFileUri = uri
            selectedFileMeta = item
        }
    }

    // Keep screen awake & high brightness while broadcasting
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
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            senderEngine.release()
            audioPlayerHelper.release()
            audioRecorderHelper.cleanup()
        }
    }

    if (senderState.isReady) {
        if (isAmbientFullscreen) {
            // AMBIENT FULLSCREEN OLED TRANSMITTER VIEW
            AmbientFullscreenBroadcastView(
                senderState = senderState,
                senderEngine = senderEngine,
                onExitFullscreen = { isAmbientFullscreen = false }
            )
        } else {
            // ACTIVE BROADCAST SCREEN
            ActiveBroadcastView(
                senderState = senderState,
                senderEngine = senderEngine,
                onClose = {
                    senderEngine.release()
                },
                onOpenMissingFramesFilter = { showMissingFramesDialog = true },
                onToggleFullscreen = { isAmbientFullscreen = true }
            )
        }

        if (showMissingFramesDialog) {
            AlertDialog(
                onDismissRequest = { showMissingFramesDialog = false },
                title = { Text("Optical Handshake: Replay Frames") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter the missing chunk numbers reported by receiver (e.g. 3, 14, 25):",
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
                        Text("Replay Selected")
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
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Bar
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

            // 2. Type Selector Tabs (Text, Link, File, Voice, Crypto)
            item {
                val types = listOf("Text", "Link", "File", "Voice", "ColdSign")
                val currentIdx = when (selectedType) {
                    TransferPayloadType.TEXT -> 0
                    TransferPayloadType.URL -> 1
                    TransferPayloadType.FILE, TransferPayloadType.MULTI_FILE -> 2
                    TransferPayloadType.AUDIO -> 3
                    TransferPayloadType.CRYPTO -> 4
                    else -> 0
                }

                GlassSegmentedControl(
                    items = types,
                    selectedIndex = currentIdx,
                    onItemSelected = { index ->
                        selectedType = when (index) {
                            0 -> TransferPayloadType.TEXT
                            1 -> TransferPayloadType.URL
                            2 -> TransferPayloadType.FILE
                            3 -> TransferPayloadType.AUDIO
                            4 -> TransferPayloadType.CRYPTO
                            else -> TransferPayloadType.TEXT
                        }
                    }
                )
            }

            // 3. Form Input Card
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = if (isDark) BentoSurfaceDark else BentoSurfaceLight,
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedType) {
                            TransferPayloadType.TEXT -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Text / Note Message",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Paste from clipboard button
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x15000000))
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    textInput = clip
                                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                                        )
                                        Text(
                                            text = "Paste",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = { Text("Enter text, notes, seed phrases, or messages...") },
                                    minLines = 4,
                                    maxLines = 8,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sender_text_input"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue,
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Web Link / URL",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x15000000))
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (!clip.isNullOrBlank()) {
                                                    urlInput = clip
                                                    Toast.makeText(context, "Pasted URL", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                                        )
                                        Text(
                                            text = "Paste",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                                        )
                                    }
                                }

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
                                        focusedBorderColor = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }

                            TransferPayloadType.AUDIO -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Offline Voice Memo / Audio Note",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Record an offline voice memo. It will be compressed, encrypted, and beamed as animated QR chunks.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isRecordingAudio) {
                                            GlassButton(
                                                text = "Stop Recording",
                                                icon = Icons.Default.Stop,
                                                isPrimary = true,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    val file = audioRecorderHelper.stopRecording()
                                                    isRecordingAudio = false
                                                    recordedAudioFile = file
                                                }
                                            )
                                        } else {
                                            GlassButton(
                                                text = if (recordedAudioFile != null) "Re-record Voice" else "Record Voice Note",
                                                icon = Icons.Default.Mic,
                                                isPrimary = recordedAudioFile == null,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    val startedFile = audioRecorderHelper.startRecording()
                                                    if (startedFile != null) {
                                                        isRecordingAudio = true
                                                    } else {
                                                        Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }

                                        if (recordedAudioFile != null && !isRecordingAudio) {
                                            GlassIconButton(
                                                icon = if (isPlayingRecordedAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play Preview",
                                                onClick = {
                                                    if (isPlayingRecordedAudio) {
                                                        audioPlayerHelper.stop()
                                                        isPlayingRecordedAudio = false
                                                    } else {
                                                        audioPlayerHelper.playFile(recordedAudioFile!!) {
                                                            isPlayingRecordedAudio = false
                                                        }
                                                        isPlayingRecordedAudio = true
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (recordedAudioFile != null) {
                                        Text(
                                            text = "Voice Memo Ready (${formatFileSize(recordedAudioFile!!.length())})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldGreen
                                        )
                                    }
                                }
                            }

                            TransferPayloadType.CRYPTO -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Cold Wallet / Raw Crypto Signer",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "Paste raw unsigned transactions, PSBT, multisig signatures, or seed phrases for 100% air-gapped transmission.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = cryptoInput,
                                        onValueChange = { cryptoInput = it },
                                        placeholder = { Text("0x02... or PSBT Base64 hex string...") },
                                        minLines = 4,
                                        maxLines = 8,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricCyan,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            }

                            TransferPayloadType.FILE, TransferPayloadType.MULTI_FILE -> {
                                Text(
                                    text = "File / Document / Photo",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val meta = selectedFileMeta
                                if (meta != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x10000000))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = meta.fileName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = formatFileSize(meta.fileSize),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        GlassIconButton(
                                            icon = Icons.Default.Close,
                                            onClick = {
                                                selectedFileUri = null
                                                selectedFileMeta = null
                                            },
                                            size = 32.dp,
                                            contentDescription = "Remove file"
                                        )
                                    }
                                } else {
                                    GlassButton(
                                        text = "Select Document or Photo",
                                        icon = Icons.Default.AttachFile,
                                        isPrimary = false,
                                        onClick = { filePicker.launch("*/*") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }

            // 4. Custom Password / PIN Encryption Options
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = if (isDark) BentoSurfaceDark else BentoSurfaceLight,
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = PurpleSecurity,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Custom Password / PIN Lock",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Receiver must enter PIN to decrypt",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isPasswordProtectionEnabled,
                                onCheckedChange = { isPasswordProtectionEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PurpleSecurity,
                                    checkedTrackColor = PurpleSecurity.copy(alpha = 0.3f)
                                )
                            )
                        }

                        AnimatedVisibility(visible = isPasswordProtectionEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = customPassword,
                                    onValueChange = { customPassword = it },
                                    label = { Text("Transfer Password / Secret PIN *") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = passwordHint,
                                    onValueChange = { passwordHint = it },
                                    label = { Text("Password Hint (Optional)") },
                                    placeholder = { Text("e.g. WiFi code, team birthday...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Time-Lock Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Time-Locked Capsule",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Access restricted until designated time",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isTimeLockEnabled,
                                onCheckedChange = { isTimeLockEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ElectricCyan,
                                    checkedTrackColor = ElectricCyan.copy(alpha = 0.3f)
                                )
                            )
                        }

                        AnimatedVisibility(visible = isTimeLockEnabled) {
                            val durations = listOf("15m", "1h", "6h", "24h", "3d", "7d")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Lock Duration:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                GlassSegmentedControl(
                                    items = durations,
                                    selectedIndex = timeLockDurationIndex,
                                    onItemSelected = { timeLockDurationIndex = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // 5. Security details pill
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

            // 6. Generate QR Broadcast Button
            item {
                val canStart = when (selectedType) {
                    TransferPayloadType.TEXT -> textInput.isNotBlank()
                    TransferPayloadType.URL -> urlInput.isNotBlank() && urlInput.length > 8
                    TransferPayloadType.AUDIO -> recordedAudioFile != null
                    TransferPayloadType.CRYPTO -> cryptoInput.isNotBlank()
                    TransferPayloadType.FILE, TransferPayloadType.MULTI_FILE -> selectedFileUri != null
                    else -> false
                } && (!isPasswordProtectionEnabled || customPassword.isNotBlank())

                if (isProcessingPayload) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                        )
                    }
                } else {
                    GlassButton(
                        text = "Generate QR Code",
                        icon = Icons.Default.QrCode2,
                        enabled = canStart,
                        isPrimary = true,
                        onClick = {
                            val durationMs = when (timeLockDurationIndex) {
                                0 -> 15 * 60 * 1000L
                                1 -> 60 * 60 * 1000L
                                2 -> 6 * 60 * 60 * 1000L
                                3 -> 24 * 60 * 60 * 1000L
                                4 -> 3 * 24 * 60 * 60 * 1000L
                                else -> 7 * 24 * 60 * 60 * 1000L
                            }
                            val timeLockUntil = if (isTimeLockEnabled) System.currentTimeMillis() + durationMs else 0L

                            isProcessingPayload = true
                            scope.launch(Dispatchers.Default) {
                                try {
                                    processAndStartTransfer(
                                        context = context,
                                        selectedType = selectedType,
                                        textInput = textInput,
                                        urlInput = urlInput,
                                        cryptoInput = cryptoInput,
                                        audioFile = recordedAudioFile,
                                        contact = ContactPayload(
                                            name = contactName,
                                            phone = contactPhone,
                                            email = contactEmail
                                        ),
                                        fileUri = selectedFileUri,
                                        fileMeta = selectedFileMeta,
                                        chunkSize = preferences.chunkSizeBytes,
                                        encrypt = preferences.encryptionEnabled,
                                        compress = preferences.compressionEnabled,
                                        customPassword = if (isPasswordProtectionEnabled) customPassword else null,
                                        passwordHint = if (isPasswordProtectionEnabled) passwordHint else null,
                                        timeLockUntil = timeLockUntil,
                                        senderEngine = senderEngine,
                                        transferRepository = transferRepository,
                                        speedMs = preferences.frameSpeedMs
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isProcessingPayload = false
                                    }
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
    onOpenMissingFramesFilter: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    val isDark = LocalIsDark.current
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
                    contentDescription = "Close Broadcast"
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
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                    )
                }

                GlassIconButton(
                    icon = Icons.Default.Fullscreen,
                    onClick = onToggleFullscreen,
                    contentDescription = "Ambient Fullscreen"
                )
            }
        }

        // Main High-Contrast QR Code Card
        item {
            BentoCard(
                modifier = Modifier.size(310.dp),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = Color.White,
                elevation = 8.dp,
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
                        CircularProgressIndicator(
                            color = if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                        )
                    }
                }
            }
        }

        // Frame Status & Controls
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (senderState.totalFrames > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame ${senderState.currentFrameIndex} of ${senderState.totalFrames}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Loops: ${senderState.completedLoops}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    GlassProgressBar(
                        progress = senderState.currentFrameIndex.toFloat() / senderState.totalFrames.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Playback Controls
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

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue)
                                .clickable { senderEngine.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (senderState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (senderState.isPlaying) "Pause" else "Play",
                                tint = if (isDark) Color(0xFF001D35) else Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        GlassIconButton(
                            icon = Icons.Default.FastForward,
                            onClick = { senderEngine.stepNext() },
                            contentDescription = "Next Frame"
                        )
                    }
                } else {
                    // Single frame static display
                    BentoPillBadge(
                        text = "STATIC QR • READY TO SCAN",
                        backgroundColor = if (isDark) BentoEmeraldDark else BentoEmerald,
                        textColor = if (isDark) EmeraldGreen else Color(0xFF065F46),
                        icon = Icons.Default.Check
                    )
                }
            }
        }

        // Live Visual Chunk Matrix
        if (senderState.totalFrames > 1) {
            item {
                ChunkMatrixGrid(
                    totalChunks = senderState.totalFrames,
                    receivedChunks = (1..senderState.totalFrames).toSet(),
                    currentActiveIndex = senderState.currentFrameIndex,
                    title = "Live Transmission Matrix"
                )
            }
        }

        // Speed / Missing Frames tuning card (only for multi-frame)
        if (senderState.totalFrames > 1) {
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = if (isDark) BentoSurfaceDark else BentoSurfaceLight,
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transmission Speed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SpeedChip(
                                    label = "Fast (100ms)",
                                    selected = senderState.speedMs <= 110,
                                    onClick = { senderEngine.setSpeedMs(100) }
                                )
                                SpeedChip(
                                    label = "Normal (160ms)",
                                    selected = senderState.speedMs in 111..200,
                                    onClick = { senderEngine.setSpeedMs(160) }
                                )
                                SpeedChip(
                                    label = "Stable (250ms)",
                                    selected = senderState.speedMs > 200,
                                    onClick = { senderEngine.setSpeedMs(250) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Optical Handshake",
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
        }

        // Paper Backup / Cold-Storage PDF Generator Button
        item {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            GlassButton(
                text = "Print Paper Backup (Cold Storage A4 PDF)",
                icon = Icons.Default.Print,
                isPrimary = false,
                onClick = {
                    val m = senderState.manifest ?: return@GlassButton
                    val frames = senderEngine.getFramesList()
                    scope.launch(Dispatchers.IO) {
                        try {
                            val pdfFile = PaperBackupHelper.generatePaperBackupPdf(context, m, frames)
                            withContext(Dispatchers.Main) {
                                PaperBackupHelper.printPdf(context, pdfFile)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "PDF generation failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Close / Done button
        item {
            GlassButton(
                text = "Done Broadcasting",
                icon = Icons.Default.Check,
                isPrimary = true,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AmbientFullscreenBroadcastView(
    senderState: com.example.transfer.SenderUiState,
    senderEngine: SenderEngine,
    onExitFullscreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frame ${senderState.currentFrameIndex}/${senderState.totalFrames}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                GlassIconButton(
                    icon = Icons.Default.FullscreenExit,
                    onClick = onExitFullscreen,
                    contentDescription = "Exit Fullscreen"
                )
            }

            // Huge Centered White QR Card
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = senderState.currentBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Full QR Frame",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassProgressBar(
                    progress = senderState.currentFrameIndex.toFloat() / senderState.totalFrames.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    height = 8.dp
                )
                Text(
                    text = "Tap screen or minimize to return",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SpeedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDark.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    if (isDark) BentoPrimaryBlueDark else BentoPrimaryBlue
                } else {
                    if (isDark) Color(0x22FFFFFF) else Color(0x10000000)
                }
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                if (isDark) Color(0xFF001D35) else Color.White
            } else MaterialTheme.colorScheme.onSurface
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
    cryptoInput: String,
    audioFile: File?,
    contact: ContactPayload,
    fileUri: Uri?,
    fileMeta: TransferFileItem?,
    chunkSize: Int,
    encrypt: Boolean,
    compress: Boolean,
    customPassword: String?,
    passwordHint: String?,
    timeLockUntil: Long = 0L,
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
        TransferPayloadType.AUDIO -> {
            val file = audioFile ?: return
            title = "Voice Memo (${formatFileSize(file.length())})"
            payloadBytes = file.readBytes()
            filesList = listOf(TransferFileItem(fileName = "voice_memo_${System.currentTimeMillis()}.m4a", fileSize = payloadBytes.size.toLong(), mimeType = "audio/mp4"))
        }
        TransferPayloadType.CRYPTO -> {
            title = "Cold Wallet Signer"
            payloadBytes = cryptoInput.toByteArray(Charsets.UTF_8)
            filesList = emptyList()
        }
        TransferPayloadType.CONTACT -> {
            title = contact.name.ifBlank { "Contact Card" }
            val vcard = contact.toVCard()
            payloadBytes = vcard.toByteArray(Charsets.UTF_8)
            filesList = emptyList()
        }
        TransferPayloadType.FILE, TransferPayloadType.MULTI_FILE -> {
            val uri = fileUri ?: return
            val meta = fileMeta ?: resolveFileMetadata(context, uri)
            title = meta.fileName
            payloadBytes = readBytesFromUri(context, uri)
            filesList = listOf(meta.copy(fileSize = payloadBytes.size.toLong()))
        }
    }

    val (manifest, frames) = QRProtocolEngine.createTransferFrames(
        type = selectedType,
        title = title,
        payloadBytes = payloadBytes,
        files = filesList,
        chunkSizeBytes = chunkSize,
        encrypt = encrypt,
        compress = compress,
        customPassword = customPassword,
        passwordHint = passwordHint,
        timeLockUntil = timeLockUntil
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
        isEncrypted = encrypt || customPassword != null,
        detailsJson = if (selectedType == TransferPayloadType.TEXT || selectedType == TransferPayloadType.URL || selectedType == TransferPayloadType.CRYPTO) String(payloadBytes, Charsets.UTF_8) else "",
        timestamp = System.currentTimeMillis()
    )
    transferRepository.saveTransfer(entity)

    senderEngine.setupTransfer(manifest, frames, initialSpeedMs = speedMs)
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray {
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.readBytes()
    } ?: ByteArray(0)
}
