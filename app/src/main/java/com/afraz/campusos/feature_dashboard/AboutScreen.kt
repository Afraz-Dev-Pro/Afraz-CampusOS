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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afraz.campusos.core.designsystem.GlassCard
import com.afraz.campusos.ui.theme.CampusPalettes
import com.afraz.campusos.ui.theme.LocalCampusColors
import com.afraz.campusos.ui.theme.ThemeViewModel
import androidx.hilt.navigation.compose.hiltViewModel

private const val GITHUB_PROFILE_URL = "https://github.com/Afraz-Dev-Pro"
private const val LINKEDIN_PROFILE_URL = "https://www.linkedin.com/in/afraz-ahamed-94601442b/"

@Composable
fun AboutScreen(modifier: Modifier = Modifier, themeViewModel: ThemeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val colors = LocalCampusColors.current
    val selectedPalette by themeViewModel.selectedPalette.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = CircleShape, color = colors.accentPrimary.copy(alpha = 0.18f)) {
            Text("CA", modifier = Modifier.padding(24.dp), color = colors.accentPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Text("CampusOS", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Developed by Afraz Ahamed", color = colors.textSecondary, fontSize = 16.sp)
        ThemePicker(selectedPalette.id, themeViewModel::selectPalette)
        GlassCard(Modifier.fillMaxWidth()) {
            Text("CONNECT", color = LocalCampusColors.current.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            SocialLink(
                title = "GitHub",
                subtitle = "@Afraz-Dev-Pro",
                enabled = true,
                icon = { Icon(Icons.Outlined.Code, contentDescription = "Source code", tint = LocalCampusColors.current.accentPrimary) },
                onClick = { openProfile(context, GITHUB_PROFILE_URL) }
            )
            SocialLink(
                title = "LinkedIn",
                subtitle = if (LINKEDIN_PROFILE_URL.isBlank()) "Profile link will be added" else "Afraz Ahamed",
                enabled = LINKEDIN_PROFILE_URL.isNotBlank(),
                icon = { Icon(Icons.Outlined.WorkOutline, contentDescription = "Project information", tint = LocalCampusColors.current.accentPrimary) },
                onClick = { if (LINKEDIN_PROFILE_URL.isNotBlank()) openProfile(context, LINKEDIN_PROFILE_URL) }
            )
        }
    }
}

@Composable
private fun ThemePicker(selectedId: String, onSelect: (String) -> Unit) {
    val colors = LocalCampusColors.current
    GlassCard(Modifier.fillMaxWidth()) {
        Text("APPEARANCE", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Choose a CampusOS color theme", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
        CampusPalettes.all.forEach { palette ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(palette.id) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = palette.id == selectedId, onClick = { onSelect(palette.id) })
                Column(Modifier.weight(1f)) {
                    Text(palette.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(if (palette.isDark) "Dark" else "Light", color = colors.textSecondary, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(palette.canvas, palette.heroCardBg, palette.accentPrimary).forEach { swatch ->
                        Surface(color = swatch, shape = CircleShape, modifier = Modifier.size(16.dp)) { }
                    }
                }
            }
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
    val colors = LocalCampusColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(end = 12.dp)) {
            androidx.compose.foundation.layout.Box(Modifier.padding(10.dp)) { icon() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
        }
        Button(onClick = onClick, enabled = enabled) { Text("Open") }
    }
}

private fun openProfile(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { if (it is ActivityNotFoundException) Toast.makeText(context, "No app can open this profile", Toast.LENGTH_LONG).show() }
}

