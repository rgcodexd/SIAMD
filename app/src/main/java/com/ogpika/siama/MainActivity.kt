package com.ogpika.siama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ogpika.siama.data.AttendanceSystemRepository
import com.ogpika.siama.data.remote.SupabaseConfig
import com.ogpika.siama.model.UserRole
import com.ogpika.siama.ui.components.SIAMATopAppBar
import com.ogpika.siama.ui.components.SupabaseConnectionDialog
import com.ogpika.siama.ui.screens.AdminPortalScreen
import com.ogpika.siama.ui.screens.InternAttendanceScreen
import com.ogpika.siama.ui.screens.LoginScreen
import com.ogpika.siama.ui.screens.OnboardingProfileScreen
import com.ogpika.siama.ui.theme.SIAMATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.init(applicationContext)
        AttendanceSystemRepository.syncWithSupabase()
        enableEdgeToEdge()
        setContent {
            val isDarkMode by AttendanceSystemRepository.isDarkMode.collectAsState()

            SIAMATheme(darkTheme = isDarkMode) {
                MainAppNavigationRoot()
            }
        }
    }
}

@Composable
fun MainAppNavigationRoot() {
    val isLoggedIn by AttendanceSystemRepository.isLoggedIn.collectAsState()
    val currentUserRole by AttendanceSystemRepository.currentUserRole.collectAsState()
    val needsOnboarding by AttendanceSystemRepository.needsOnboarding.collectAsState()
    val isDarkMode by AttendanceSystemRepository.isDarkMode.collectAsState()
    var showSupabaseDialog by remember { mutableStateOf(false) }

    Crossfade(targetState = isLoggedIn to needsOnboarding, label = "authCrossfade") { (loggedIn, onboarding) ->
        if (!loggedIn) {
            LoginScreen(
                isDarkMode = isDarkMode,
                onToggleTheme = { AttendanceSystemRepository.toggleTheme() }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    SIAMATopAppBar(
                        currentRole = currentUserRole,
                        isDarkMode = isDarkMode,
                        onToggleTheme = { AttendanceSystemRepository.toggleTheme() },
                        onOpenSupabaseConfig = { showSupabaseDialog = true },
                        onLogout = { AttendanceSystemRepository.logout() }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentUserRole) {
                        UserRole.ADMIN -> {
                            AdminPortalScreen()
                        }
                        UserRole.INTERN -> {
                            if (onboarding) {
                                OnboardingProfileScreen()
                            } else {
                                InternAttendanceScreen()
                            }
                        }
                        null -> {
                            LoginScreen(
                                isDarkMode = isDarkMode,
                                onToggleTheme = { AttendanceSystemRepository.toggleTheme() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSupabaseDialog) {
        SupabaseConnectionDialog(onDismiss = { showSupabaseDialog = false })
    }
}