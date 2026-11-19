package com.example.uwbextracredit4377

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.uwbextracredit4377.ui.TrackerDashboard
import com.example.uwbextracredit4377.ui.TrackerViewModel
import com.example.uwbextracredit4377.ui.theme.UWBExtraCredit4377Theme

class MainActivity : ComponentActivity() {

    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep screen awake while tracking UWB
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        setContent {
            UWBExtraCredit4377Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TrackerApp(
                        viewModel = viewModel,
                        checkPermissions = { hasRequiredPermissions() }
                    )
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        fun getRequiredPermissions(): Array<String> {
            return arrayOf(
                Manifest.permission.UWB_RANGING,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        }
    }
}

@Composable
fun TrackerApp(
    viewModel: TrackerViewModel,
    checkPermissions: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasPermissions by remember { mutableStateOf(checkPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        hasPermissions = allGranted
        if (!allGranted) {
            viewModel.toggleTracking(false)
        }
    }

    // Cleanly request permissions on startup
    LaunchedEffect(Unit) {
        val permissions = MainActivity.getRequiredPermissions()
        val missing = permissions.filter { !checkPermissions() }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(permissions)
        }
    }

    TrackerDashboard(
        uiState = uiState,
        onToggleTracking = { isActive ->
            if (isActive) {
                if (!hasPermissions) {
                    permissionLauncher.launch(MainActivity.getRequiredPermissions())
                } else {
                    viewModel.toggleTracking(true)
                }
            } else {
                viewModel.toggleTracking(false)
            }
        },
        onPeerAddressChanged = { newAddress ->
            viewModel.setPeerAddress(newAddress)
        },
        onRequestPermissions = {
            permissionLauncher.launch(MainActivity.getRequiredPermissions())
        },
        hasPermissions = hasPermissions,
        modifier = modifier
    )
}
