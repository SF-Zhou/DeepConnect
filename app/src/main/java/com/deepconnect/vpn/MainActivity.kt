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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepconnect.vpn.ui.AppViewModel
import com.deepconnect.vpn.ui.DeepConnectTheme
import com.deepconnect.vpn.ui.EditProfileScreen
import com.deepconnect.vpn.ui.HomeScreen
import com.deepconnect.vpn.ui.LogScreen
import com.deepconnect.vpn.ui.PromptDialogs
import com.deepconnect.vpn.ui.SettingsScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LOGS = "logs"
private const val ROUTE_EDIT = "edit/{profileId}"

private fun editRoute(profileId: Long): String = "edit/$profileId"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeepConnectTheme {
                // Only a genuine launcher start (ACTION_MAIN) may trigger auto-connect.
                AppRoot(fromLauncher = intent?.action == Intent.ACTION_MAIN)
            }
        }
    }
}

@Composable
private fun AppRoot(
    fromLauncher: Boolean,
    viewModel: AppViewModel = viewModel(),
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
    // Only a genuine launcher start may connect: otherwise any app that fires an explicit
    // intent at this activity could make us bring the tunnel up.
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

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(ROUTE_HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onAddProfile = { navController.navigate(editRoute(0L)) },
                    onEditProfile = { profileId -> navController.navigate(editRoute(profileId)) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onOpenLogs = { navController.navigate(ROUTE_LOGS) },
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
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenLogs = { navController.navigate(ROUTE_LOGS) },
                )
            }
            composable(ROUTE_LOGS) {
                LogScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Certificate trust / one-time-password prompts block the native handshake, so they
        // float above whatever screen is currently visible.
        PromptDialogs(prompt = prompt)
    }
}
