package com.afraz.campusos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afraz.campusos.core.designsystem.CampusOsTheme
import com.afraz.campusos.core.designsystem.GlassBorderBottom
import com.afraz.campusos.core.designsystem.GlassBorderTop
import com.afraz.campusos.core.designsystem.GlassFill
import com.afraz.campusos.feature_academics.AcademicsScreen
import com.afraz.campusos.feature_dashboard.DashboardScreen
import com.afraz.campusos.feature_hackhub.HackHubScreen
import com.afraz.campusos.feature_toolkit.ToolkitScreen
import com.afraz.campusos.feature_toolkit.tile.QuickScanTileService
import com.afraz.campusos.navigation.CampusRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var openScannerRequest = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openScannerRequest.value = intent.getBooleanExtra(QuickScanTileService.EXTRA_OPEN_SCANNER, false)
        setContent {
            CampusOsTheme {
                CampusShell(openScannerRequest.value, onScannerConsumed = { openScannerRequest.value = false }, onScannerRequested = { openScannerRequest.value = true })
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(QuickScanTileService.EXTRA_OPEN_SCANNER, false)) openScannerRequest.value = true
    }
}

@Composable
private fun CampusShell(openScannerRequest: Boolean, onScannerConsumed: () -> Unit, onScannerRequested: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    LaunchedEffect(openScannerRequest) {
        if (openScannerRequest) navController.navigate(CampusRoute.Toolkit) { launchSingleTop = true }
    }
    val tabs = listOf(
        "Dashboard" to CampusRoute.Dashboard,
        "Academics" to CampusRoute.Academics,
        "Toolkit" to CampusRoute.Toolkit,
        "HackHub" to CampusRoute.HackHub
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            androidx.compose.material3.Surface(
                color = GlassFill,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
                    .border(1.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)), RoundedCornerShape(26.dp))
            ) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent, tonalElevation = 0.dp) {
                    tabs.forEach { (label, route) ->
                        val selected = currentRoute == route::class.qualifiedName || currentRoute?.contains(route::class.simpleName.orEmpty()) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {},
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) { insets ->
        NavHost(navController = navController, startDestination = CampusRoute.Dashboard, modifier = Modifier.padding(insets)) {
            composable<CampusRoute.Dashboard> {
                DashboardScreen(
                    onOpenToolkit = { navController.navigate(CampusRoute.Toolkit) { launchSingleTop = true } },
                    onOpenScanner = { navController.navigate(CampusRoute.Toolkit) { launchSingleTop = true }; onScannerRequested() },
                    onOpenAcademics = { navController.navigate(CampusRoute.Academics) { launchSingleTop = true } }
                )
            }
            composable<CampusRoute.Academics> { AcademicsScreen() }
            composable<CampusRoute.Toolkit> {
                ToolkitScreen(autoStartScanner = openScannerRequest, onAutoStartConsumed = onScannerConsumed)
            }
            composable<CampusRoute.HackHub> { HackHubScreen() }
        }
    }
}
