package com.deepconnect.vpn

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepconnect.vpn.ui.AppViewModel
import com.deepconnect.vpn.ui.components.AuroraBackdrop
import com.deepconnect.vpn.ui.components.AuroraBottomBar
import com.deepconnect.vpn.ui.components.AuroraTab
import com.deepconnect.vpn.ui.components.PromptDialogs
import com.deepconnect.vpn.ui.screens.EditProfileScreen
import com.deepconnect.vpn.ui.screens.HomeScreen
import com.deepconnect.vpn.ui.screens.LogsScreen
import com.deepconnect.vpn.ui.screens.SettingsScreen
import com.deepconnect.vpn.ui.theme.DeepConnectTheme

private const val ROUTE_EDIT = "edit/{profileId}"

private fun editRoute(profileId: Long): String = "edit/$profileId"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            DeepConnectTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                // Only a genuine launcher start (ACTION_MAIN) may trigger auto-connect.
                AppRoot(viewModel = viewModel, fromLauncher = intent?.action == Intent.ACTION_MAIN)
            }
        }
    }
}

@Composable
private fun AppRoot(
    viewModel: AppViewModel,
    fromLauncher: Boolean,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Ask for the system VPN consent dialog when the controller needs it, then resume the
    // connect request that triggered it. The ViewModel keeps the profile until the result.
    val consentIntent by viewModel.consentIntent.collectAsState()
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(consentIntent) {
        val intent = consentIntent
        if (intent != null) {
            viewModel.onConsentLaunched()
            consentLauncher.launch(intent)
        }
    }

    // "Auto-connect on launch": runs at most once per app process, after the consent
    // launcher above is ready so a missing VPN permission can be requested normally.
    LaunchedEffect(Unit) {
        viewModel.maybeAutoConnectOnLaunch(context, fromLauncher)
    }

    // The persistent VPN notification is how the user controls the tunnel from outside the
    // app, so ask for POST_NOTIFICATIONS once (Android 13+).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        var permissionRequested by rememberSaveable { mutableStateOf(false) }
        val notificationLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { /* The notification is a convenience; the VPN works without it. */ }
        LaunchedEffect(Unit) {
            if (!permissionRequested) {
                permissionRequested = true
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    val prompt by viewModel.prompt.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = AuroraTab.entries.firstOrNull { tab -> tab.route == currentRoute }

    AuroraBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (selectedTab != null) {
                    AuroraBottomBar(
                        selected = selectedTab,
                        onSelect = { tab -> navController.navigateToTab(tab) },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AuroraTab.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
                composable(AuroraTab.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onAddProfile = { navController.navigate(editRoute(0L)) },
                        onEditProfile = { profileId -> navController.navigate(editRoute(profileId)) },
                    )
                }
                composable(AuroraTab.Logs.route) {
                    LogsScreen(viewModel = viewModel)
                }
                composable(AuroraTab.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenLogs = { navController.navigateToTab(AuroraTab.Logs) },
                        onOpenProfiles = { navController.navigateToTab(AuroraTab.Home) },
                    )
                }
                composable(
                    route = ROUTE_EDIT,
                    arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
                ) { entry ->
                    EditProfileScreen(
                        viewModel = viewModel,
                        profileId = entry.arguments?.getLong("profileId") ?: 0L,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        // Certificate trust / one-time-password prompts block the native handshake, so they
        // float above whatever screen is currently visible.
        PromptDialogs(prompt = prompt)
    }
}

/** Bottom-navigation style tab switch with state preservation. */
private fun NavHostController.navigateToTab(tab: AuroraTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
