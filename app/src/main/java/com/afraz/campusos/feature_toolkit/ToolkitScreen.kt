package com.afraz.campusos.feature_toolkit

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary
import com.afraz.campusos.data.WalletDocEntity
import com.afraz.campusos.feature_toolkit.focus.PomodoroForegroundService
import com.afraz.campusos.feature_toolkit.focus.PomodoroForegroundService.Companion.ACTION_PAUSE
import com.afraz.campusos.feature_toolkit.focus.PomodoroForegroundService.Companion.ACTION_SKIP
import com.afraz.campusos.feature_toolkit.focus.PomodoroForegroundService.Companion.ACTION_START
import com.afraz.campusos.feature_toolkit.focus.PomodoroForegroundService.Companion.ACTION_STOP
import com.afraz.campusos.feature_toolkit.focus.PomodoroForegroundService.Companion.sendAction
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(modifier: Modifier = Modifier, autoStartScanner: Boolean = false, onAutoStartConsumed: () -> Unit = {}, walletOnly: Boolean = false, viewModel: ToolkitViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val haptics = LocalHapticFeedback.current
    var scanMessage by remember { mutableStateOf("") }
    var pdfTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var imagePdfTitle by remember { mutableStateOf("Study notes") }
    var expenseTitle by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Canteen") }
    var expenseAmount by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var friendName by remember { mutableStateOf("") }
    var friendUpi by remember { mutableStateOf("") }
    var groupTitle by remember { mutableStateOf("") }
    var groupAmount by remember { mutableStateOf("") }
    var customShares by remember { mutableStateOf("") }
    var peopleSplitAmount by remember { mutableStateOf("") }
    var peopleCount by remember { mutableStateOf("2") }
    var calculatedPeopleShares by remember { mutableStateOf<List<Long>?>(null) }
    var selectedFriends by remember { mutableStateOf(setOf<String>()) }
    var capturedPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var focusSeconds by remember { mutableStateOf(25 * 60) }
    var focusActive by remember { mutableStateOf(false) }
    var focusPaused by remember { mutableStateOf(false) }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportBackup) }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::restoreBackup) }
    LaunchedEffect(focusActive, focusPaused) {
        while (focusActive && !focusPaused && focusSeconds > 0) {
            kotlinx.coroutines.delay(1_000)
            focusSeconds -= 1
        }
        if (focusSeconds == 0) focusActive = false
    }
    val postNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) PomodoroForegroundService.sendAction(context, ACTION_START)
    }
    val startFocus = {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            postNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else PomodoroForegroundService.sendAction(context, ACTION_START)
        Unit
    }

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            var saved = 0
            scan?.pdf?.uri?.let { viewModel.importDocument(it, "Scanned notes", "Scans"); saved++ }
            scan?.pages?.forEachIndexed { index, page -> viewModel.importDocument(page.imageUri, "Scanned page ${index + 1}", "Scans"); saved++ }
            scanMessage = if (saved > 0) "$saved scan file(s) saved to your wallet" else "No scanned pages returned"
        } else {
            scanMessage = "Scan cancelled"
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) viewModel.createImagePdf(imagePdfTitle, uris)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val captured = pendingCameraUri
        if (saved && captured != null) capturedPhotos = capturedPhotos + captured
        pendingCameraUri = null
    }
    val takePhoto: () -> Unit = {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val photo = File(directory, "pdf-page-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photo)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
        Unit
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importDocument(it, it.lastPathSegment?.substringAfterLast('/') ?: "Campus document") }
    }
    val launchScanner: () -> Unit = {
        val activity = context.findActivity()
        GmsDocumentScanning.getClient(scannerOptions()).getStartScanIntent(activity)
            .addOnSuccessListener { scannerLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { scanMessage = it.localizedMessage ?: "Scanner could not start" }
        Unit
    }
    LaunchedEffect(autoStartScanner) {
        if (autoStartScanner) {
            launchScanner()
            onAutoStartConsumed()
        }
    }
    var viewingDoc by remember { mutableStateOf<WalletDocEntity?>(null) }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(if (walletOnly) "Digital ID" else "Tools", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(if (walletOnly) "Your campus documents, ready when you need them." else "Campus helpers that work offline.", color = TextSecondary)
        }
        if (!walletOnly) item { Text("STUDY", color = TextSecondary, fontWeight = FontWeight.Bold) }
        if (!walletOnly) item {
            GlassCard {
                Text("DOCUMENT SCANNER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Google Play services scans pages without a camera permission.", color = TextPrimary)
                Button(onClick = launchScanner) { Text("Scan notes") }
                if (scanMessage.isNotBlank()) Text(scanMessage, color = TextSecondary)
            }
        }
        if (!walletOnly) item {
            GlassCard {
                Text("NATIVE PDF MAKER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(pdfTitle, { pdfTitle = it }, label = { Text("PDF title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(noteText, { noteText = it }, label = { Text("Notes · separate pages with ---PAGE---") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                Button(onClick = { viewModel.createNotesPdf(pdfTitle.ifBlank { "Notes" }, noteText); pdfTitle = ""; noteText = "" }, enabled = noteText.isNotBlank()) { Text("Create A4 PDF from notes") }
                OutlinedTextField(imagePdfTitle, { imagePdfTitle = it }, label = { Text("Image PDF title") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Choose images and create PDF") }
                Button(onClick = takePhoto) { Text("Take photo for PDF") }
                if (capturedPhotos.isNotEmpty()) {
                    Text("${capturedPhotos.size} photo(s) captured. Take more pages or create the PDF.", color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.createImagePdf(imagePdfTitle.ifBlank { "Scanned pages" }, capturedPhotos); capturedPhotos = emptyList() }) { Text("Create PDF from photos") }
                        Button(onClick = { capturedPhotos = emptyList() }) { Text("Clear photos") }
                    }
                }
                Button(onClick = { documentPicker.launch(arrayOf("application/pdf", "image/*")) }) { Text("Import PDF or ID document") }
            }
        }
        if (!walletOnly) item {
            GlassCard {
                Text("POMODORO FOCUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(if (focusActive) if (focusPaused) "Focus paused" else "Focus session active" else "Ready for a focus block", color = TextPrimary)
                Text("%02d:%02d".format(focusSeconds / 60, focusSeconds % 60), color = TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("Controls also stay in your notification shade.", color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); focusSeconds = 25 * 60; focusActive = true; focusPaused = false; startFocus() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Start") }
                    Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); if (!focusActive) focusActive = true; focusPaused = !focusPaused; PomodoroForegroundService.sendAction(context, ACTION_PAUSE) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (focusPaused) "Resume" else "Pause") }
                    Button(onClick = { PomodoroForegroundService.sendAction(context, ACTION_SKIP); focusSeconds = 25 * 60; focusActive = false; focusPaused = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Skip") }
                    Button(onClick = { PomodoroForegroundService.sendAction(context, ACTION_STOP); focusActive = false; focusPaused = false; focusSeconds = 25 * 60 }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stop") }
                }
            }
        }
        if (!walletOnly) item { Text("CAMPUS", color = TextSecondary, fontWeight = FontWeight.Bold) }
        if (!walletOnly) item {
            GlassCard {
                Text("DIGITAL ID & CAMPUS PASSES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { documentPicker.launch(arrayOf("image/*", "application/pdf")) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add ID or pass") }
                state.documents.forEach { doc ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${doc.docType} · ${doc.subjectFolder ?: "Campus"}", color = TextSecondary, fontSize = 12.sp)
                        }
                        Button(onClick = { if (doc.mimeType.startsWith("image/")) viewingDoc = doc else openPdf(context, doc.localFilePath) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open") }
                    }
                }
            }
        }
        if (!walletOnly) item { Text("DATA", color = TextSecondary, fontWeight = FontWeight.Bold) }
        if (!walletOnly) item {
            GlassCard {
                Text("LOCAL BACKUP", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Export or restore your local campus data as JSON using the Android file picker.", color = TextPrimary)
                Button(onClick = { backupPicker.launch("campusos_backup_v1.json") }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Export JSON") }
                Button(onClick = { restorePicker.launch(arrayOf("application/json", "application/gzip", "application/octet-stream")) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Restore JSON") }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
    viewingDoc?.let { WalletImageDialog(it, onClose = { viewingDoc = null }) }
}

@Composable
private fun WalletImageDialog(doc: WalletDocEntity, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var highBrightness by remember { mutableStateOf(false) }
    val originalBrightness = remember(activity) { activity.window.attributes.screenBrightness }
    DisposableEffect(highBrightness, activity) {
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = if (highBrightness) 1f else originalBrightness }
        onDispose { activity.window.attributes = activity.window.attributes.apply { screenBrightness = originalBrightness } }
    }
    val bitmap by produceState<android.graphics.Bitmap?>(null, doc.localFilePath) {
        value = withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeFile(doc.localFilePath) }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    val transform = rememberTransformableState { zoom, _, _ -> scale = (scale * zoom).coerceIn(1f, 5f) }
    Dialog(onDismissRequest = onClose) {
        Surface {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { highBrightness = !highBrightness }) { Text(if (highBrightness) "Brightness on" else "Max brightness") }
                    Button(onClick = onClose) { Text("Close") }
                }
                bitmap?.let { image ->
                    Image(image.asImageBitmap(), contentDescription = doc.title, modifier = Modifier.weight(1f).fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale).transformable(transform).pointerInput(Unit) { })
                } ?: Text("Image unavailable")
            }
        }
    }
}

private fun scannerOptions() = GmsDocumentScannerOptions.Builder()
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .setGalleryImportAllowed(true)
    .setPageLimit(30)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
    .build()

private fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("An Activity context is required")
}

private fun openPdf(context: Context, path: String) {
    val file = File(path)
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer is installed", Toast.LENGTH_LONG).show()
    }
}

