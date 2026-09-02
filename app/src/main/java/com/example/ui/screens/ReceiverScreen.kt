package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.TransferRepository
import com.example.data.UserPreferencesRepository
import com.example.protocol.TransferPayloadType
import com.example.util.MediaCategory
import com.example.util.MediaExportHelper
import com.example.scanner.ImageQRDecoder
import com.example.scanner.QRCodeAnalyzer
import com.example.transfer.ReceivedTransferResult
import com.example.transfer.ReceiverEngine
import com.example.transfer.ReceiverStateStatus
import com.example.ui.components.AirGapBadge
import com.example.ui.components.ChunkMatrixGrid
import com.example.ui.components.EncryptionBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassProgressBar
import com.example.ui.components.MissingFramesHandshakeDialog
import com.example.ui.components.PasswordPromptDialog
import com.example.ui.components.ScannerOverlay
import com.example.ui.components.VoiceNotePlayerCard
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.CyanBlueGradient
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReceiverScreen(
    transferRepository: TransferRepository,
    preferencesRepository: UserPreferencesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDark.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val receiverEngine = remember {
        ReceiverEngine(context, transferRepository, scope)
    }
    val receiverState by receiverEngine.uiState.collectAsState()

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var showMissingHandshakeDialog by remember { mutableStateOf(false) }
    var showMatrixExpanded by remember { mutableStateOf(false) }

    // Haptic feedback trigger on successful frame / complete
    fun triggerHaptic(strong: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = if (strong) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(if (strong) 80L else 20L)
            }
        } catch (_: Exception) {}
    }

    // Pick QR image from Gallery
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val decodedText = ImageQRDecoder.decodeFromUri(context, uri)
                if (decodedText != null) {
                    val accepted = receiverEngine.onFrameScanned(decodedText)
                    if (accepted) {
                        triggerHaptic(strong = true)
                    } else {
                        Toast.makeText(context, "QR code is not a valid DropQR transfer", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No QR code found in selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            receiverEngine.reset()
        }
    }

    LaunchedEffect(receiverState.status) {
        if (receiverState.status == ReceiverStateStatus.SUCCESS) {
            triggerHaptic(strong = true)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (receiverState.status == ReceiverStateStatus.SUCCESS && receiverState.result != null) {
            // SUCCESS RESULT SCREEN
            TransferSuccessView(
                result = receiverState.result!!,
                onDone = {
                    receiverEngine.reset()
                    onBack()
                },
                onScanAnother = {
                    receiverEngine.reset()
                }
            )
        } else if (!cameraPermissionState.status.isGranted) {
            // CAMERA PERMISSION REQUEST VIEW
            CameraPermissionPromptView(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onBack = onBack
            )
        } else {
            // LIVE CAMERA SCANNER VIEW
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(
                                    cameraExecutor,
                                    QRCodeAnalyzer { rawQrText ->
                                        val accepted = receiverEngine.onFrameScanned(rawQrText)
                                        if (accepted) {
                                            triggerHaptic(strong = false)
                                        }
                                    }
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraInstance = camera
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning Overlay & Live Progress HUD
            ScannerOverlay(
                isTorchOn = isTorchOn,
                onToggleTorch = {
                    val target = !isTorchOn
                    isTorchOn = target
                    cameraInstance?.cameraControl?.enableTorch(target)
                },
                onPickImage = { galleryPicker.launch("image/*") },
                onClose = {
                    receiverEngine.cancelTransfer()
                    onBack()
                },
                statusText = when (receiverState.status) {
                    ReceiverStateStatus.IDLE -> "Point camera at sender's animated QR"
                    ReceiverStateStatus.CONNECTING -> "Transfer detected. Connecting..."
                    ReceiverStateStatus.RECEIVING -> "Receiving Frame ${receiverState.receivedFramesCount} of ${receiverState.totalFrames}"
                    ReceiverStateStatus.VERIFYING -> "Verifying cryptographic checksums..."
                    ReceiverStateStatus.DECRYPTING -> "Decrypting AES-256-GCM payload..."
                    ReceiverStateStatus.AWAITING_PASSWORD -> "Protected transfer: PIN/Password required"
                    ReceiverStateStatus.SAVING -> "Saving received data..."
                    ReceiverStateStatus.ERROR -> receiverState.errorMessage
                    ReceiverStateStatus.SUCCESS -> "Transfer Complete!"
                }
            )

            // Live Floating HUD during active reception
            if (receiverState.status == ReceiverStateStatus.RECEIVING ||
                receiverState.status == ReceiverStateStatus.VERIFYING ||
                receiverState.status == ReceiverStateStatus.DECRYPTING ||
                receiverState.status == ReceiverStateStatus.AWAITING_PASSWORD ||
                receiverState.status == ReceiverStateStatus.SAVING
            ) {
                LiveReceptionCard(
                    receiverState = receiverState,
                    showMatrixExpanded = showMatrixExpanded,
                    onToggleMatrix = { showMatrixExpanded = !showMatrixExpanded },
                    onRequestMissingHandshake = { showMissingHandshakeDialog = true },
                    onCancel = { receiverEngine.cancelTransfer() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                )
            }

            // Error Toast / Alert
            if (receiverState.status == ReceiverStateStatus.ERROR) {
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color(0xDD200505)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Transfer Failed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = receiverState.errorMessage,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                        GlassButton(
                            text = "Rescan",
                            icon = Icons.Default.Replay,
                            onClick = { receiverEngine.reset() }
                        )
                    }
                }
            }
        }

        // Password Prompt Dialog
        if (receiverState.status == ReceiverStateStatus.AWAITING_PASSWORD) {
            PasswordPromptDialog(
                hint = receiverState.passwordHint,
                errorMessage = receiverState.passwordError,
                onConfirm = { pinOrPass ->
                    scope.launch {
                        receiverEngine.submitPassword(pinOrPass)
                    }
                },
                onDismiss = {
                    receiverEngine.cancelTransfer()
                }
            )
        }

        // Missing Frames Handshake Request QR Dialog
        if (showMissingHandshakeDialog && receiverState.missingFrames.isNotEmpty()) {
            MissingFramesHandshakeDialog(
                transferId = receiverState.transferId,
                missingFrames = receiverState.missingFrames,
                onDismiss = { showMissingHandshakeDialog = false }
            )
        }
    }
}

@Composable
private fun LiveReceptionCard(
    receiverState: com.example.transfer.ReceiverUiState,
    showMatrixExpanded: Boolean,
    onToggleMatrix: () -> Unit,
    onRequestMissingHandshake: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color(0xD00A1128),
        elevation = 14.dp
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Transferring [${receiverState.transferId}]",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "${receiverState.progressPercent}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }

            GlassProgressBar(
                progress = receiverState.progressPercent / 100f,
                height = 8.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${receiverState.receivedFramesCount} / ${receiverState.totalFrames} frames",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )

                if (receiverState.missingFrames.isNotEmpty() && receiverState.receivedFramesCount > 0) {
                    Text(
                        text = "${receiverState.missingFrames.size} missing",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleSecurity
                    )
                }

                Text(
                    text = String.format("%.1f fps", receiverState.speedFps),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EmeraldGreen
                )
            }

            // Interactive Chunk Matrix
            if (receiverState.totalFrames > 0) {
                ChunkMatrixGrid(
                    totalChunks = receiverState.totalFrames,
                    receivedChunks = receiverState.receivedFrameIndices,
                    currentActiveIndex = receiverState.lastScannedFrameIndex,
                    title = "Live Frame Beam Status"
                )
            }

            // Two-Way Optical Handshake button if some frames missing
            if (receiverState.missingFrames.isNotEmpty() && receiverState.receivedFramesCount > 0) {
                GlassButton(
                    text = "Request Missing Frames (${receiverState.missingFrames.size})",
                    icon = Icons.Default.Sync,
                    isPrimary = false,
                    onClick = onRequestMissingHandshake,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TransferSuccessView(
    result: ReceivedTransferResult,
    onDone: () -> Unit,
    onScanAnother: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Header Checkmark
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(EmeraldGreen.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        .border(2.dp, EmeraldGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "Transfer Complete",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Data verified & decrypted locally without internet",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = result.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Type: ${result.type.displayName} • ${result.totalFrames} frames",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EncryptionBadge()
                    }

                    // Content Preview
                    when (result.type) {
                        TransferPayloadType.AUDIO -> {
                            val path = result.audioFilePath
                            if (path != null) {
                                val audioFile = File(path)
                                VoiceNotePlayerCard(
                                    audioFile = audioFile,
                                    title = "Decrypted Voice Memo"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassButton(
                                        text = "Save to Music",
                                        icon = Icons.Default.Download,
                                        isPrimary = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            val res = MediaExportHelper.saveFileToDevice(
                                                context = context,
                                                sourceFile = audioFile,
                                                originalFileName = audioFile.name.substringAfter('_', audioFile.name),
                                                mimeType = "audio/m4a"
                                            )
                                            Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                        }
                                    )

                                    GlassButton(
                                        text = "Share",
                                        icon = Icons.Default.Share,
                                        isPrimary = false,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            shareFile(context, audioFile, "audio/m4a")
                                        }
                                    )
                                }
                            }
                        }

                        TransferPayloadType.CRYPTO -> {
                            val text = result.textContent ?: ""
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Cold Wallet / Air-Gapped Signature Payload",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ElectricCyan
                                    )
                                }
                                Text(
                                    text = text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        TransferPayloadType.TEXT, TransferPayloadType.URL -> {
                            val text = result.textContent ?: ""
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassButton(
                                    text = "Copy Text",
                                    icon = Icons.Default.ContentCopy,
                                    isPrimary = false,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("DropQR", text))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )

                                if (result.type == TransferPayloadType.URL) {
                                    GlassButton(
                                        text = "Open Link",
                                        icon = Icons.Default.OpenInBrowser,
                                        isPrimary = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(text))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        TransferPayloadType.CONTACT -> {
                            val contact = result.contactData
                            if (contact != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "Name: ${contact.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (contact.phone.isNotBlank()) Text(text = "Phone: ${contact.phone}", fontSize = 13.sp)
                                    if (contact.email.isNotBlank()) Text(text = "Email: ${contact.email}", fontSize = 13.sp)
                                    if (contact.organization.isNotBlank()) Text(text = "Org: ${contact.organization}", fontSize = 13.sp)
                                }
                            }
                        }

                        TransferPayloadType.FILE, TransferPayloadType.MULTI_FILE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                result.savedFiles.forEach { fileItem ->
                                    val file = File(fileItem.localPath)
                                    val category = MediaExportHelper.determineCategory(fileItem.name, fileItem.mimeType)
                                    val saveLabel = when (category) {
                                        MediaCategory.IMAGE, MediaCategory.VIDEO -> "Save to Gallery"
                                        MediaCategory.AUDIO -> "Save to Music"
                                        MediaCategory.DOCUMENT -> "Save to Downloads"
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = ElectricCyan
                                                )
                                                Column {
                                                    Text(
                                                        text = fileItem.name,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${fileItem.size} bytes • ${category.label}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                GlassIconButton(
                                                    icon = Icons.Default.Download,
                                                    size = 36.dp,
                                                    contentDescription = saveLabel,
                                                    tint = EmeraldGreen,
                                                    onClick = {
                                                        val res = MediaExportHelper.saveFileToDevice(
                                                            context = context,
                                                            sourceFile = file,
                                                            originalFileName = fileItem.name,
                                                            mimeType = fileItem.mimeType
                                                        )
                                                        Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                                    }
                                                )

                                                GlassIconButton(
                                                    icon = Icons.Default.OpenInNew,
                                                    size = 36.dp,
                                                    contentDescription = "Open file",
                                                    onClick = {
                                                        MediaExportHelper.openInSystemViewer(context, file, fileItem.mimeType)
                                                    }
                                                )

                                                GlassIconButton(
                                                    icon = Icons.Default.Share,
                                                    size = 36.dp,
                                                    contentDescription = "Share",
                                                    onClick = {
                                                        shareFile(context, file, fileItem.mimeType)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (result.savedFiles.isNotEmpty()) {
                    val primary = result.savedFiles.first()
                    val primaryFile = File(primary.localPath)
                    val cat = MediaExportHelper.determineCategory(primary.name, primary.mimeType)
                    val mainSaveLabel = if (result.savedFiles.size == 1) {
                        when (cat) {
                            MediaCategory.IMAGE, MediaCategory.VIDEO -> "Save to Device Gallery"
                            MediaCategory.AUDIO -> "Save to Device Music"
                            MediaCategory.DOCUMENT -> "Save to Device Downloads"
                        }
                    } else {
                        "Save All Files to Device Storage"
                    }

                    GlassButton(
                        text = mainSaveLabel,
                        icon = Icons.Default.Download,
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            var savedCount = 0
                            var lastMsg = ""
                            result.savedFiles.forEach { item ->
                                val res = MediaExportHelper.saveFileToDevice(
                                    context = context,
                                    sourceFile = File(item.localPath),
                                    originalFileName = item.name,
                                    mimeType = item.mimeType
                                )
                                if (res.isSuccess) {
                                    savedCount++
                                    lastMsg = res.message
                                }
                            }
                            if (result.savedFiles.size > 1) {
                                Toast.makeText(context, "Saved $savedCount files to device storage!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, lastMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    GlassButton(
                        text = "Share Transferred File",
                        icon = Icons.Default.Share,
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            shareFile(context, primaryFile, primary.mimeType)
                        }
                    )
                }

                GlassButton(
                    text = "Scan Another Transfer",
                    icon = Icons.Default.QrCodeScanner,
                    isPrimary = result.savedFiles.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onScanAnother
                )

                GlassButton(
                    text = "Done",
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDone
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionPromptView(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                contentDescription = "Back"
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AppleBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = AppleBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "DropQR needs camera access to scan and stream encrypted QR frames in real-time from sender devices.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                AirGapBadge()

                GlassButton(
                    text = "Enable Camera",
                    isPrimary = true,
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open share chooser", Toast.LENGTH_SHORT).show()
    }
}

