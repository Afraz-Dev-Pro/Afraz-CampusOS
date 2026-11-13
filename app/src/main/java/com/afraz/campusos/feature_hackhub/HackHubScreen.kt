package com.afraz.campusos.feature_hackhub

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary
import java.time.Instant
import java.time.temporal.ChronoUnit

private val skillTags = listOf("Kotlin", "Compose", "Python", "C++", "Web", "AI/ML", "CyberSec", "UI/UX")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HackHubScreen(modifier: Modifier = Modifier, viewModel: HackHubViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val state by viewModel.state.collectAsState()
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportBackup) }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::restoreBackup) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HackHubEvent.ExportBackup -> backupPicker.launch("campusos_backup_v1.json")
            }
        }
    }
    var author by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var pitch by remember { mutableStateOf("") }
    var github by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("HackHub", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Find teammates, events and past papers.", color = TextSecondary)
        }
        item {
            GlassCard {
                Text("HACKATHONS & SPONSOR BOUNTIES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (state.usingSeedData) Text("Showing bundled starter events · remote feed will replace these when available", color = TextSecondary, fontSize = 12.sp)
                if (state.loading) CircularProgressIndicator()
                state.hackathons.forEach { event ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                        Text(event.eventName, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${event.startsAt.take(10)} – ${event.endsAt.take(10)} · ${event.prizePool}", color = TextSecondary)
                        Text("Registration: ${countdown(event.registrationDeadline)}", color = TextPrimary, fontSize = 12.sp)
                        Text("Sponsor tracks: ${event.sponsorTracks.joinToString()}", color = TextSecondary, fontSize = 12.sp)
                        Button(onClick = { openExternal(context, event.applyUrl) }) { Text("Apply Now") }
                    }
                }
            }
        }
        item {
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TEAMMATE REQUESTS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = viewModel::refreshTeammates) { Text("Refresh") }
                }
                Text("Posts expire after 14 days. Open profiles or contact links to connect.", color = TextSecondary, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { viewModel.selectSkill(null) }) { Text("All") }
                    skillTags.forEach { tag ->
                        if (tag in skillTags.take(3)) Button(onClick = { viewModel.selectSkill(if (state.selectedSkill == tag) null else tag) }) { Text(if (state.selectedSkill == tag) "✓ $tag" else tag) }
                    }
                }
                Button(onClick = viewModel::requestPost) { Text("Post Teammate Request") }
                val visiblePosts = state.teammatePosts.filter { post ->
                    state.selectedSkill == null || post.skills.any { it.equals(state.selectedSkill, true) }
                }
                if (visiblePosts.isEmpty() && !state.loading) Text("No active teammate requests yet.", color = TextSecondary)
                visiblePosts.forEach { post -> TeammateCard(post) { openExternal(context, connectUrl(post)) } }
                Button(onClick = viewModel::loadMore, enabled = state.teammatePosts.isNotEmpty() && !state.loadingMore) { Text(if (state.loadingMore) "Loading…" else "Load more") }
            }
        }
        item {
            GlassCard {
                Text("BACKUP & RESTORE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Export or restore all 11 local tables through the Android file picker. Choose Google Drive there to store a copy.", color = TextPrimary)
                Button(onClick = viewModel::requestBackup) { Text("Cloud Backup / Export") }
                Button(onClick = { restorePicker.launch(arrayOf("application/json", "application/gzip", "application/octet-stream")) }) { Text("Restore from file") }
            }
        }
        state.error?.let { error -> item { Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error) } }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (state.showAuthSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissAuth) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sign in when you need community or backup", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Your timetable, attendance and toolkit remain available offline without an account.", color = TextSecondary)
                Button(onClick = { viewModel.signIn(activity) }) { Text("Continue with Google") }
                state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (state.showPostForm) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissPostForm) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Post a teammate request", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                OutlinedTextField(author, { author = it.take(40) }, label = { Text("Name (max 40)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(college, { college = it.take(60) }, label = { Text("College (max 60)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pitch, { pitch = it.take(150) }, label = { Text("Project pitch (max 150)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(github, { github = it.filter(Char::isLetterOrDigit).take(39) }, label = { Text("GitHub username") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(contact, { contact = it.take(200) }, label = { Text("Contact URL (optional)") }, modifier = Modifier.fillMaxWidth())
                skillTags.forEach { skill -> Button(onClick = { selectedSkills = if (skill in selectedSkills) selectedSkills - skill else if (selectedSkills.size < 8) selectedSkills + skill else selectedSkills }) { Text(if (skill in selectedSkills) "✓ $skill" else skill) } }
                Button(onClick = { viewModel.submitPost(author, college, pitch, selectedSkills.toList(), github, contact) }, enabled = author.isNotBlank() && college.isNotBlank() && pitch.isNotBlank() && selectedSkills.isNotEmpty()) { Text("Publish request") }
                state.error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun TeammateCard(post: TeammatePost, onConnect: () -> Unit) {
    Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (post.githubUsername.isNotBlank()) {
                    AsyncImage(model = "https://github.com/${post.githubUsername}.png", contentDescription = "${post.authorName} avatar", modifier = Modifier.clip(CircleShape).height(42.dp))
                }
                Column {
                    Text(post.authorName, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(post.college, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Text(post.pitchBio, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
            Text(post.skills.joinToString(" · "), color = TextSecondary, fontSize = 12.sp)
            Button(onClick = onConnect) { Text("Connect") }
        }
    }
}

private fun connectUrl(post: TeammatePost): String = when {
    post.githubUsername.isNotBlank() -> "https://github.com/${post.githubUsername}"
    post.contactUrl.startsWith("https://") || post.contactUrl.startsWith("http://") -> post.contactUrl
    else -> "https://github.com/"
}

private fun openExternal(context: Context, value: String) {
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return
    if (uri.scheme !in setOf("https", "http")) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure {
        if (it is ActivityNotFoundException) android.widget.Toast.makeText(context, "No app can open this link", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun countdown(date: String): String = runCatching {
    val remaining = ChronoUnit.HOURS.between(Instant.now(), Instant.parse(date))
    if (remaining < 0) "Registration closed" else "${remaining / 24}d ${remaining % 24}h left"
}.getOrDefault(date)

private fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("An Activity context is required")
}
