package com.afraz.campusos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afraz.campusos.core.designsystem.GlassBorderBottom
import com.afraz.campusos.core.designsystem.GlassBorderTop
import com.afraz.campusos.core.designsystem.GlassFill
import com.afraz.campusos.feature_academics.AcademicsScreen
import com.afraz.campusos.feature_dashboard.AboutScreen
import com.afraz.campusos.feature_dashboard.HomeScreen
import com.afraz.campusos.feature_hackhub.HackHubScreen
import com.afraz.campusos.feature_toolkit.FinanceScreen
import com.afraz.campusos.feature_toolkit.ToolkitScreen
import com.afraz.campusos.feature_toolkit.tile.QuickScanTileService
import com.afraz.campusos.navigation.CampusRoute
import com.afraz.campusos.ui.theme.CampusOsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openScannerRequest = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(7, 8, 12)),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(7, 8, 12))
        )
        openScannerRequest.value = intent.getBooleanExtra(QuickScanTileService.EXTRA_OPEN_SCANNER, false)
        setContent {
            CampusOsTheme {
                CampusNavScaffold(
                    openScannerRequest = openScannerRequest.value,
                    onScannerConsumed = { openScannerRequest.value = false },
                    onScannerRequested = { openScannerRequest.value = true }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(QuickScanTileService.EXTRA_OPEN_SCANNER, false)) {
            openScannerRequest.value = true
        }
    }
}

private data class CampusDestination(
    val label: String,
    val route: CampusRoute,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun CampusNavScaffold(
    openScannerRequest: Boolean,
    onScannerConsumed: () -> Unit,
    onScannerRequested: () -> Unit
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route.orEmpty()
    val isAboutPage = currentRoute.substringAfterLast('.').startsWith("About")
    val destinations = listOf(
        CampusDestination("Home", CampusRoute.Dashboard, Icons.Outlined.Home),
        CampusDestination("Classes", CampusRoute.Academics, Icons.Outlined.School),
        CampusDestination("Finance", CampusRoute.Finance, Icons.Outlined.AccountBalanceWallet),
        CampusDestination("Tools", CampusRoute.Toolkit, Icons.Outlined.Build),
        CampusDestination("HackHub", CampusRoute.HackHub, Icons.Outlined.Groups)
    )

    LaunchedEffect(openScannerRequest) {
        if (openScannerRequest) {
            navController.navigate(CampusRoute.Toolkit) { launchSingleTop = true }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAboutPage) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(if (isAboutPage) "About CampusOS" else "CampusOS", fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    if (!isAboutPage) {
                        IconButton(onClick = {
                            navController.navigate(CampusRoute.About) { launchSingleTop = true }
                        }) {
                            Icon(Icons.Outlined.Info, contentDescription = "About CampusOS")
                        }
                    }
                }
            },
            bottomBar = {
                if (compact && !isAboutPage) {
                    androidx.compose.material3.Surface(
                        color = GlassFill,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom)),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = isSelected(currentRoute, destination.route),
                                    onClick = { navController.navigateTo(destination.route) },
                                    icon = {
                                        Icon(
                                            destination.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            destination.label,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontSize = 10.sp
                                        )
                                    },
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                }
            }
        ) { contentInsets ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentInsets)
            ) {
                if (!compact && !isAboutPage) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            NavigationRailItem(
                                selected = isSelected(currentRoute, destination.route),
                                onClick = { navController.navigateTo(destination.route) },
                                icon = {
                                    Icon(destination.icon, contentDescription = null)
                                },
                                label = { Text(destination.label, maxLines = 1, softWrap = false) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = CampusRoute.Dashboard,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<CampusRoute.Dashboard> {
                        HomeScreen(
                            onOpenClasses = { navController.navigateTo(CampusRoute.Academics) },
                            onOpenImport = { navController.navigateTo(CampusRoute.Academics) },
                            onOpenProfile = { navController.navigateTo(CampusRoute.About) },
                            onOpenFinance = { navController.navigateTo(CampusRoute.Finance) },
                            onOpenTools = { navController.navigateTo(CampusRoute.Toolkit) },
                            onOpenScanner = {
                                navController.navigateTo(CampusRoute.Toolkit)
                                onScannerRequested()
                            }
                        )
                    }
                    composable<CampusRoute.Academics> { AcademicsScreen() }
                    composable<CampusRoute.Toolkit> {
                        ToolkitScreen(
                            autoStartScanner = openScannerRequest,
                            onAutoStartConsumed = onScannerConsumed
                        )
                    }
                    composable<CampusRoute.HackHub> { HackHubScreen() }
                    composable<CampusRoute.Finance> { FinanceScreen() }
                    composable<CampusRoute.DigitalId> { ToolkitScreen(walletOnly = true) }
                    composable<CampusRoute.About> { AboutScreen() }
                }
            }
        }
    }
}

private fun isSelected(currentRoute: String, route: CampusRoute): Boolean =
    currentRoute.substringAfterLast('.').startsWith(route::class.simpleName.orEmpty())

private fun androidx.navigation.NavHostController.navigateTo(route: CampusRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
