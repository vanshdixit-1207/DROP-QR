package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.stego.ExtractedSecret
import com.example.stego.SteganographyEngine
import com.example.ui.components.AirGapBadge
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoPillBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassSegmentedControl
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIsDark
import com.example.ui.theme.PurpleSecurity
import com.example.util.MediaExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteganographyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Hide (Encode), 1: Reveal (Decode)

    // Encode State
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var secretText by remember { mutableStateOf("") }
    var secretFileUri by remember { mutableStateOf<Uri?>(null) }
    var secretFileName by remember { mutableStateOf("") }
    var secretFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var secretMimeType by remember { mutableStateOf("text/plain") }

    var isPasswordEnabled by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordHint by remember { mutableStateOf("") }

    var isTimeLockEnabled by remember { mutableStateOf(false) }
    var timeLockDurationIndex by remember { mutableStateOf(0) } // 0: 15m, 1: 1h, 2: 6h, 3: 24h, 4: 3d

    var isEncoding by remember { mutableStateOf(false) }
    var encodedResultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var encodedSavedFile by remember { mutableStateOf<File?>(null) }

    // Decode State
    var decodeImageUri by remember { mutableStateOf<Uri?>(null) }
    var decodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isDecoding by remember { mutableStateOf(false) }
    var decodePassword by remember { mutableStateOf("") }
    var decodePasswordRequired by remember { mutableStateOf(false) }
    var decodePasswordHint by remember { mutableStateOf("") }
    var extractedSecret by remember { mutableStateOf<ExtractedSecret?>(null) }
    var decodeError by remember { mutableStateOf<String?>(null) }

    // Pickers
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coverUri = uri
            scope.launch(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    withContext(Dispatchers.Main) {
                        coverBitmap = bmp
                        encodedResultBitmap = null
                        encodedSavedFile = null
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Could not load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val secretFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            secretFileUri = uri
            scope.launch(Dispatchers.IO) {
                try {
                    var fName = "secret_file"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fName = cursor.getString(nameIndex)
                        }
                    }
                    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

                    withContext(Dispatchers.Main) {
                        secretFileName = fName
                        secretMimeType = mime
                        secretFileBytes = bytes
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val decodePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            decodeImageUri = uri
            extractedSecret = null
            decodeError = null
            decodePasswordRequired = false
            scope.launch(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    withContext(Dispatchers.Main) {
                        decodeBitmap = bmp
                    }
                    // Auto-attempt decode
                    performDecode(context, bmp, null) { res, err, reqPwd, hint ->
                        extractedSecret = res
                        decodeError = err
                        decodePasswordRequired = reqPwd
                        decodePasswordHint = hint
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        decodeError = "Failed to load image: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x33FFFFFF) else Color(0x11000000))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        Text(
                            text = "Optical Steganography",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Ghost Mode • Hide files inside photos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AirGapBadge()
            }
        }

        // Tab Selector
        item {
            GlassSegmentedControl(
                items = listOf("Hide Secret (Encode)", "Reveal Secret (Decode)"),
                selectedIndex = selectedTabIndex,
                onItemSelected = {
                    selectedTabIndex = it
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (selectedTabIndex == 0) {
            // ================= ENCODE / HIDE =================
            // 1. Cover Photo Selection
            item {
                BentoCard(
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
                                text = "1. Cover Image (Normal Photo)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            BentoPillBadge(
                                text = if (coverBitmap != null) "IMAGE READY" else "SELECT COVER",
                                backgroundColor = if (coverBitmap != null) EmeraldGreen.copy(alpha = 0.2f) else Color(0x1A000000),
                                textColor = if (coverBitmap != null) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                icon = Icons.Default.Image
                            )
                        }

                        if (coverBitmap != null) {
                            val capacity = SteganographyEngine.calculateCapacityBytes(coverBitmap!!.width, coverBitmap!!.height)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x0A000000))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    bitmap = coverBitmap!!.asImageBitmap(),
                                    contentDescription = "Cover Image",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Resolution: ${coverBitmap!!.width} × ${coverBitmap!!.height}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Stego Capacity: ~${capacity / 1024} KB of secret data",
                                        fontSize = 12.sp,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                IconButton(onClick = { coverPickerLauncher.launch("image/*") }) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Change Cover",
                                        tint = ElectricCyan
                                    )
                                }
                            }
                        } else {
                            GlassButton(
                                text = "Choose Photo from Gallery",
                                icon = Icons.Default.PhotoLibrary,
                                isPrimary = false,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { coverPickerLauncher.launch("image/*") }
                            )
                        }
                    }
                }
            }

            // 2. Secret Data Input
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Secret Payload to Hide",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = secretText,
                            onValueChange = {
                                secretText = it
                                if (it.isNotBlank()) {
                                    secretFileUri = null
                                    secretFileBytes = null
                                }
                            },
                            label = { Text("Secret Message / Password / Note") },
                            placeholder = { Text("Type any confidential text, recovery seed, or message...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "OR Hide Any File:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            GlassButton(
                                text = if (secretFileBytes != null) secretFileName.take(16) else "Pick Secret File",
                                icon = Icons.Default.AttachFile,
                                isPrimary = secretFileBytes != null,
                                onClick = { secretFilePickerLauncher.launch("*/*") }
                            )
                        }

                        if (secretFileBytes != null) {
                            Text(
                                text = "Attached: $secretFileName (${secretFileBytes!!.size / 1024} KB)",
                                fontSize = 12.sp,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Security & Time-Lock Options
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "3. Security & Time Lock",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Password Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PIN / Password Protection",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Requires PIN to extract secret from photo",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isPasswordEnabled,
                                onCheckedChange = { isPasswordEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PurpleSecurity)
                            )
                        }

                        AnimatedVisibility(visible = isPasswordEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Secret Password / PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = passwordHint,
                                    onValueChange = { passwordHint = it },
                                    label = { Text("Password Hint (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Time Lock Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Time-Locked Capsule",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cannot be opened before designated time",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isTimeLockEnabled,
                                onCheckedChange = { isTimeLockEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                            )
                        }

                        AnimatedVisibility(visible = isTimeLockEnabled) {
                            val durations = listOf("15m", "1h", "6h", "24h", "3 Days", "7 Days")
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

            // 4. Action Button
            item {
                val hasPayload = secretText.isNotBlank() || secretFileBytes != null
                val canEncode = coverBitmap != null && hasPayload && (!isPasswordEnabled || password.isNotBlank())

                GlassButton(
                    text = if (isEncoding) "Invisibly Embedding Data..." else "Embed Secret Inside Photo (Ghost Mode)",
                    icon = Icons.Default.Lock,
                    isPrimary = true,
                    enabled = canEncode && !isEncoding,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val bmp = coverBitmap ?: return@GlassButton
                        val secretBytes = if (secretFileBytes != null) {
                            secretFileBytes!!
                        } else {
                            secretText.toByteArray(Charsets.UTF_8)
                        }
                        val fileName = if (secretFileBytes != null) secretFileName else "secret_note.txt"
                        val mime = if (secretFileBytes != null) secretMimeType else "text/plain"

                        val durationMs = when (timeLockDurationIndex) {
                            0 -> 15 * 60 * 1000L
                            1 -> 60 * 60 * 1000L
                            2 -> 6 * 60 * 60 * 1000L
                            3 -> 24 * 60 * 60 * 1000L
                            4 -> 3 * 24 * 60 * 60 * 1000L
                            else -> 7 * 24 * 60 * 60 * 1000L
                        }
                        val timeLockUntil = if (isTimeLockEnabled) System.currentTimeMillis() + durationMs else 0L

                        isEncoding = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val stegoBmp = SteganographyEngine.hideSecretInBitmap(
                                    coverBitmap = bmp,
                                    title = "Hidden Secret",
                                    fileName = fileName,
                                    mimeType = mime,
                                    secretBytes = secretBytes,
                                    customPassword = if (isPasswordEnabled) password else null,
                                    passwordHint = if (isPasswordEnabled) passwordHint else null,
                                    timeLockUntil = timeLockUntil
                                )

                                val saved = SteganographyEngine.saveStegoBitmapToPng(context, stegoBmp)
                                MediaExportHelper.saveFileToDevice(
                                    context = context,
                                    sourceFile = saved,
                                    originalFileName = saved.name,
                                    mimeType = "image/png"
                                )

                                withContext(Dispatchers.Main) {
                                    encodedResultBitmap = stegoBmp
                                    encodedSavedFile = saved
                                    isEncoding = false
                                    Toast.makeText(context, "Secret successfully embedded into photo & saved to Gallery!", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isEncoding = false
                                    Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }

            // 5. Result View
            if (encodedResultBitmap != null && encodedSavedFile != null) {
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = EmeraldGreen.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(36.dp)
                            )

                            Text(
                                text = "Stego Photo Generated!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )

                            Text(
                                text = "This photo looks 100% normal in any phone or gallery, but contains your encrypted secret payload!",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Image(
                                bitmap = encodedResultBitmap!!.asImageBitmap(),
                                contentDescription = "Generated Stego Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassButton(
                                    text = "Share Photo",
                                    icon = Icons.Default.Share,
                                    isPrimary = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", encodedSavedFile!!)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Stego Image"))
                                    }
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // ================= DECODE / REVEAL =================
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Select Image to Scan for Hidden Secrets",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        GlassButton(
                            text = if (decodeBitmap != null) "Pick Another Image" else "Choose Image from Gallery",
                            icon = Icons.Default.PhotoLibrary,
                            isPrimary = decodeBitmap == null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { decodePickerLauncher.launch("image/*") }
                        )

                        if (decodeBitmap != null) {
                            Image(
                                bitmap = decodeBitmap!!.asImageBitmap(),
                                contentDescription = "Scanned Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        if (decodePasswordRequired) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "🔒 Password Protected Secret",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleSecurity
                                )
                                if (decodePasswordHint.isNotBlank()) {
                                    Text(
                                        text = "Hint: $decodePasswordHint",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedTextField(
                                    value = decodePassword,
                                    onValueChange = { decodePassword = it },
                                    label = { Text("Enter PIN / Decryption Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                GlassButton(
                                    text = "Unlock & Extract Secret",
                                    icon = Icons.Default.Key,
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val bmp = decodeBitmap ?: return@GlassButton
                                        performDecode(context, bmp, decodePassword) { res, err, reqPwd, hint ->
                                            extractedSecret = res
                                            decodeError = err
                                            decodePasswordRequired = reqPwd
                                            decodePasswordHint = hint
                                        }
                                    }
                                )
                            }
                        }

                        if (decodeError != null) {
                            Text(
                                text = decodeError ?: "",
                                fontSize = 13.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Extracted Result
            if (extractedSecret != null) {
                val secret = extractedSecret!!
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (secret.isLockedByTime) ElectricCyan.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.12f)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (secret.isLockedByTime) Icons.Default.AccessTime else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (secret.isLockedByTime) ElectricCyan else EmeraldGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (secret.isLockedByTime) "Time-Locked Capsule" else "Hidden Secret Extracted!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (secret.isLockedByTime) ElectricCyan else EmeraldGreen
                                    )
                                }

                                BentoPillBadge(
                                    text = "${secret.metadata.fileSize} B",
                                    backgroundColor = Color(0x33FFFFFF),
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (secret.isLockedByTime) {
                                TimeLockCountdownView(
                                    targetTimestamp = secret.metadata.timeLockUntil,
                                    onUnlocked = {
                                        val bmp = decodeBitmap ?: return@TimeLockCountdownView
                                        performDecode(context, bmp, decodePassword) { res, err, reqPwd, hint ->
                                            extractedSecret = res
                                            decodeError = err
                                        }
                                    }
                                )
                            } else {
                                if (secret.textContent != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x0A000000))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "SECRET NOTE / MESSAGE:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = secret.textContent,
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    GlassButton(
                                        text = "Copy Secret Text",
                                        icon = Icons.Default.ContentCopy,
                                        isPrimary = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Secret Text", secret.textContent))
                                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else if (secret.savedFilePath != null) {
                                    Text(
                                        text = "Extracted File: ${secret.metadata.fileName}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    GlassButton(
                                        text = "Export / Open File",
                                        icon = Icons.Default.FileOpen,
                                        isPrimary = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            val file = File(secret.savedFilePath)
                                            MediaExportHelper.saveFileToDevice(
                                                context = context,
                                                sourceFile = file,
                                                originalFileName = secret.metadata.fileName,
                                                mimeType = secret.metadata.mimeType
                                            )
                                            Toast.makeText(context, "Saved to Downloads/DropQR: ${secret.metadata.fileName}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun performDecode(
    context: Context,
    bitmap: Bitmap,
    password: String?,
    onResult: (ExtractedSecret?, String?, Boolean, String) -> Unit
) {
    try {
        if (!SteganographyEngine.hasHiddenData(bitmap)) {
            onResult(null, "No hidden DropQR secret found in this image.", false, "")
            return
        }
        val res = SteganographyEngine.extractSecretFromBitmap(context, bitmap, password)
        onResult(res, null, false, "")
    } catch (e: com.example.protocol.PasswordRequiredException) {
        onResult(null, null, true, e.hint)
    } catch (e: com.example.protocol.IncorrectPasswordException) {
        onResult(null, "Incorrect password / PIN. Please try again.", true, "")
    } catch (e: Exception) {
        onResult(null, "Extraction failed: ${e.localizedMessage}", false, "")
    }
}

@Composable
fun TimeLockCountdownView(
    targetTimestamp: Long,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeRemainingMs by remember { mutableStateOf(maxOf(0L, targetTimestamp - System.currentTimeMillis())) }

    LaunchedEffect(targetTimestamp) {
        while (timeRemainingMs > 0L) {
            delay(1000L)
            timeRemainingMs = maxOf(0L, targetTimestamp - System.currentTimeMillis())
            if (timeRemainingMs == 0L) {
                onUnlocked()
                break
            }
        }
    }

    val seconds = (timeRemainingMs / 1000) % 60
    val minutes = (timeRemainingMs / (1000 * 60)) % 60
    val hours = (timeRemainingMs / (1000 * 60 * 60)) % 24
    val days = timeRemainingMs / (1000 * 60 * 60 * 24)

    val timeString = if (days > 0) {
        String.format(Locale.getDefault(), "%dd %02dh %02dm %02ds", days, hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    val unlockDate = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(targetTimestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElectricCyan.copy(alpha = 0.15f))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Time Capsule Countdown",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
        }

        Text(
            text = timeString,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Unlocks on: $unlockDate",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
