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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ogpika.siama.ui.components.AppRole
import com.ogpika.siama.ui.components.SailTopBar
import com.ogpika.siama.ui.screens.AdminManagementScreen
import com.ogpika.siama.ui.screens.InternDashboardScreen
import com.ogpika.siama.ui.theme.SAILInternAttendanceManagementApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SAILInternAttendanceManagementApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    var currentRole by remember { mutableStateOf(AppRole.INTERN) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        topBar = {
            SailTopBar(
                currentRole = currentRole,
                onRoleToggle = { newRole ->
                    currentRole = newRole
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            Crossfade(
                targetState = currentRole,
                label = "roleCrossfade"
            ) { role ->
                when (role) {
                    AppRole.INTERN -> {
                        InternDashboardScreen()
                    }
                    AppRole.ADMIN -> {
                        AdminManagementScreen()
                    }
                }
            }
        }
    }
}