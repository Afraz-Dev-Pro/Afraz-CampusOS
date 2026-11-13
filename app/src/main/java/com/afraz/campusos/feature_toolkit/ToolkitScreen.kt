package com.afraz.campusos.feature_toolkit

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.afraz.campusos.core.util.ExpenseMath
import com.afraz.campusos.core.util.UpiDeepLinkHelper
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
import java.text.DateFormat
import java.util.Calendar
import kotlin.math.max

@Composable
fun ToolkitScreen(modifier: Modifier = Modifier, autoStartScanner: Boolean = false, onAutoStartConsumed: () -> Unit = {}, financeOnly: Boolean = false, walletOnly: Boolean = false, viewModel: ToolkitViewModel = hiltViewModel()) {
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
            Text(if (financeOnly) "Finance" else if (walletOnly) "Digital ID" else "Toolkit", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(if (financeOnly) "Personal spending, UPI and bill splits." else if (walletOnly) "Your campus documents, ready when you need them." else "Campus helpers that work offline.", color = TextSecondary)
        }
        if (!financeOnly && !walletOnly) item {
            GlassCard {
                Text("DOCUMENT SCANNER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Google Play services scans pages without a camera permission.", color = TextPrimary)
                Button(onClick = launchScanner) { Text("Scan notes") }
                if (scanMessage.isNotBlank()) Text(scanMessage, color = TextSecondary)
            }
        }
        if (!financeOnly && !walletOnly) item {
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
        if (financeOnly) item {
            val monthStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val spent = state.expenses.filter { it.timestamp >= monthStart }.sumOf { it.amountPaise }
            GlassCard {
                Text("PERSONAL SPEND", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("₹${"%.2f".format(spent / 100.0)} spent this month · ₹${"%.2f".format(state.monthlyBudgetPaise / 100.0)} budget", color = TextPrimary)
                androidx.compose.material3.LinearProgressIndicator(progress = { if (state.monthlyBudgetPaise == 0L) 1f else (spent.toFloat() / state.monthlyBudgetPaise).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(expenseTitle, { expenseTitle = it }, label = { Text("Item") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(expenseAmount, { expenseAmount = it }, label = { Text("₹ amount") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(expenseCategory, { expenseCategory = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.addExpense(expenseTitle, expenseCategory, expenseAmount); expenseTitle = ""; expenseAmount = "" }, enabled = expenseTitle.isNotBlank() && expenseAmount.isNotBlank()) { Text("Add expense") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(budget, { budget = it }, label = { Text("Monthly budget ₹") }, modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.setMonthlyBudget(budget); budget = "" }) { Text("Set") }
                }
                state.expenses.take(5).forEach { expense ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${expense.title} · ${expense.category}", color = TextPrimary)
                        Text("₹${"%.2f".format(expense.amountPaise / 100.0)}", color = TextSecondary)
                    }
                }
            }
        }
        if (financeOnly) item {
            GlassCard {
                Text("SPLIT BY PEOPLE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Divide any bill evenly without saving contacts or UPI IDs.", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(peopleSplitAmount, { peopleSplitAmount = it; calculatedPeopleShares = null }, label = { Text("Total ₹") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(peopleCount, { peopleCount = it.filter(Char::isDigit).take(3); calculatedPeopleShares = null }, label = { Text("People") }, modifier = Modifier.weight(1f))
                }
                val splitTotalPaise = runCatching { ExpenseMath.rupeesToPaise(peopleSplitAmount) }.getOrNull()
                val count = peopleCount.toIntOrNull()
                Button(
                    onClick = { calculatedPeopleShares = runCatching { ExpenseMath.splitPaise(splitTotalPaise!!, count!!) }.getOrNull() },
                    enabled = splitTotalPaise != null && splitTotalPaise > 0L && count != null && count in 1..100
                ) { Text("Calculate share") }
                calculatedPeopleShares?.let { shares ->
                    val distribution = shares.groupingBy { it }.eachCount().entries
                        .sortedBy { it.key }
                        .joinToString(" · ") { (paise, people) -> "₹${"%.2f".format(paise / 100.0)} × $people" }
                    Text(if (shares.distinct().size == 1) "₹${"%.2f".format(shares.first() / 100.0)} each · ${shares.size} people" else "Per-person shares: $distribution", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    if (shares.distinct().size > 1) Text("Extra paise are distributed so the shares add up to the exact total.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        if (financeOnly) item {
            GlassCard {
                Text("FRIENDS & BILL SPLITTER", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(friendName, { friendName = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(friendUpi, { friendUpi = it }, label = { Text("UPI ID") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = { viewModel.saveFriend(friendName, friendUpi); friendName = ""; friendUpi = "" }, enabled = friendName.isNotBlank() && friendUpi.contains('@')) { Text("Save friend") }
                if (friendName.isNotBlank() && !friendUpi.contains('@')) {
                    Text("Add a UPI ID (for example, name@bank) to enable Save friend.", color = TextSecondary, fontSize = 12.sp)
                }
                state.friends.forEach { friend ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(friend.id in selectedFriends, onCheckedChange = { checked -> selectedFriends = if (checked) selectedFriends + friend.id else selectedFriends - friend.id })
                        Text("${friend.name} · ${friend.upiId}", color = TextPrimary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(groupTitle, { groupTitle = it }, label = { Text("Bill note") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(groupAmount, { groupAmount = it }, label = { Text("Total ₹") }, modifier = Modifier.weight(1f))
                }
                val validSplitTotal = groupAmount.trim().toBigDecimalOrNull()?.let { it > java.math.BigDecimal.ZERO } == true
                when {
                    state.friends.isEmpty() -> Text("To split a bill, first save a friend with a valid UPI ID above.", color = TextSecondary, fontSize = 12.sp)
                    selectedFriends.isEmpty() -> Text("Select at least one saved friend. The total will be divided evenly between you and the selected friends.", color = TextSecondary, fontSize = 12.sp)
                    !validSplitTotal && groupAmount.isNotBlank() -> Text("Enter a total greater than ₹0.", color = TextSecondary, fontSize = 12.sp)
                    selectedFriends.isNotEmpty() -> Text("Splitting the total across you and ${selectedFriends.size} selected friend(s).", color = TextSecondary, fontSize = 12.sp)
                }
                Button(onClick = { viewModel.createEqualSplit(groupTitle, groupAmount, selectedFriends.toList()); groupTitle = ""; groupAmount = "" }, enabled = groupTitle.isNotBlank() && validSplitTotal && selectedFriends.isNotEmpty()) { Text("Split equally") }
                Text("Custom split: enter one ₹ share per selected friend, in the list order. Any remainder stays with you.", color = TextSecondary, fontSize = 12.sp)
                OutlinedTextField(customShares, { customShares = it }, label = { Text("Custom friend shares, comma separated") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val ordered = state.friends.filter { it.id in selectedFriends }.map { it.id }
                    viewModel.createCustomSplit(groupTitle, groupAmount, ordered, customShares.split(',').map(String::trim))
                    groupTitle = ""; groupAmount = ""; customShares = ""
                }, enabled = groupTitle.isNotBlank() && groupAmount.isNotBlank() && selectedFriends.isNotEmpty() && customShares.isNotBlank()) { Text("Save custom split") }
                state.groups.forEach { group ->
                    Text("${group.title} · ₹${"%.2f".format(group.totalAmountPaise / 100.0)}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    state.splits.filter { it.groupId == group.id }.forEach { split ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${split.friendName} · ₹${"%.2f".format(split.shareAmountPaise / 100.0)}", color = TextPrimary)
                                Text(if (split.isSettled) "Settled" else "Unsettled", color = TextSecondary, fontSize = 12.sp)
                            }
                            if (!split.isSettled) {
                                Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); UpiDeepLinkHelper.settle(context, split.friendUpiId, split.friendName, group.title, split.shareAmountPaise) }) { Text("Settle Up") }
                                Button(onClick = { viewModel.setSettled(split.id, true) }) { Text("Paid") }
                            }
                        }
                    }
                }
            }
        }
        if (!financeOnly) item {
            GlassCard {
                Text("DIGITAL ID & DOCUMENT WALLET", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { documentPicker.launch(arrayOf("image/*", "application/pdf")) }) { Text("Add ID, pass or hall ticket") }
                state.documents.forEach { doc ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(doc.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${doc.docType} · ${doc.subjectFolder ?: "Campus"}", color = TextSecondary, fontSize = 12.sp)
                        }
                        Button(onClick = {
                            if (doc.mimeType.startsWith("image/")) viewingDoc = doc else openPdf(context, doc.localFilePath)
                        }) { Text("Open") }
                        Button(onClick = { viewModel.deleteDocument(doc.id) }) { Text("Remove") }
                    }
                }
            }
        }
        if (!financeOnly && !walletOnly) item {
            GlassCard {
                Text("POMODORO FOCUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Start a 25 minute focus block. Controls stay in your notification shade.", color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); startFocus() }) { Text("Start") }
                    Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); PomodoroForegroundService.sendAction(context, ACTION_PAUSE) }) { Text("Pause / resume") }
                    Button(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); PomodoroForegroundService.sendAction(context, ACTION_SKIP) }) { Text("Skip") }
                    Button(onClick = { PomodoroForegroundService.sendAction(context, ACTION_STOP) }) { Text("Stop") }
                }
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
