package com.afraz.campusos.feature_dashboard

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afraz.campusos.core.designsystem.AccentIndigo
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.core.designsystem.TextPrimary
import com.afraz.campusos.core.designsystem.TextSecondary

private const val GITHUB_PROFILE_URL = "https://github.com/Afraz-Dev-Pro"
private const val LINKEDIN_PROFILE_URL = "https://www.linkedin.com/in/afraz-ahamed-94601442b/"

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = AccentIndigo.copy(alpha = 0.18f)) {
            Text("CA", modifier = Modifier.padding(24.dp), color = AccentIndigo, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text("CampusOS", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Developed by Afraz Ahamed", color = TextSecondary, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("CONNECT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SocialLink(
                title = "GitHub",
                subtitle = "@Afraz-Dev-Pro",
                enabled = true,
                icon = { Icon(Icons.Outlined.Code, contentDescription = null, tint = AccentIndigo) },
                onClick = { openProfile(context, GITHUB_PROFILE_URL) }
            )
            SocialLink(
                title = "LinkedIn",
                subtitle = if (LINKEDIN_PROFILE_URL.isBlank()) "Profile link will be added" else "Afraz Ahamed",
                enabled = LINKEDIN_PROFILE_URL.isNotBlank(),
                icon = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = AccentIndigo) },
                onClick = { if (LINKEDIN_PROFILE_URL.isNotBlank()) openProfile(context, LINKEDIN_PROFILE_URL) }
            )
        }
    }
}

@Composable
private fun SocialLink(
    title: String,
    subtitle: String,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(end = 12.dp)) {
            androidx.compose.foundation.layout.Box(Modifier.padding(10.dp)) { icon() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Button(onClick = onClick, enabled = enabled) { Text("Open") }
    }
}

private fun openProfile(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { if (it is ActivityNotFoundException) Toast.makeText(context, "No app can open this profile", Toast.LENGTH_LONG).show() }
}
