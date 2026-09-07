package com.clipboardmemory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clipboardmemory.service.ClipboardService
import com.clipboardmemory.ui.ChatScreen
import com.clipboardmemory.ui.ClipboardScreen
import com.clipboardmemory.ui.ClipboardViewModel
import com.clipboardmemory.ui.theme.ClipboardMemoryTheme
import com.clipboardmemory.util.Preferences

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Preferences.isServiceEnabled(this) && !Preferences.isServiceRunning(this)) {
            ClipboardService.start(this)
        }

        setContent {
            ClipboardMemoryTheme {
                MainScaffold(
                    onRequestNotificationsPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(onRequestNotificationsPermission: () -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val clipboardViewModel: ClipboardViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    label = { Text("Clipboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    label = { Text("Ask") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> ClipboardScreen(
                viewModel = clipboardViewModel,
                onRequestNotificationsPermission = onRequestNotificationsPermission,
                modifier = Modifier.padding(padding)
            )
            1 -> ChatScreen(modifier = Modifier.padding(padding))
        }
    }
}