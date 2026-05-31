package com.gp.q733.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gp.q733.presentation.navigation.Q733NavHost
import com.gp.q733.presentation.theme.GPQ733Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var permissionCallback: ((Boolean) -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        permissionCallback?.invoke(allGranted)
        permissionCallback = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 延迟初始化，避免启动时立即访问蓝牙
        setContent {
            GPQ733Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionScreen()
                }
            }
        }
    }
    @Composable
    private fun PermissionScreen() {
        var permissionState by remember { mutableStateOf(PermissionState.CHECKING) }
        LaunchedEffect(Unit) {
            // 延迟检查权限，确保Activity完全初始化
            kotlinx.coroutines.delay(500)
            val hasPermissions = checkBluetoothPermissions()
            permissionState = if (hasPermissions) {
                PermissionState.GRANTED
            } else {
                PermissionState.REQUESTING
            }
        }
        when (permissionState) {
            PermissionState.CHECKING -> {
                LoadingScreen("正在检查权限...")
            }
            PermissionState.REQUESTING -> {
                RequestPermissionScreen(
                    onRequestPermission = {
                        requestPermissions { granted ->
                            permissionState = if (granted) {
                                PermissionState.GRANTED
                            } else {
                                PermissionState.DENIED
                            }
                        }
                    }
                )
            }
            PermissionState.DENIED -> {
                PermissionDeniedScreen(
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    },
                    onRetry = {
                        permissionState = PermissionState.REQUESTING
                    }
                )
            }
            PermissionState.GRANTED -> {
                Q733NavHost()
            }
        }
    }
    @Composable
    private fun LoadingScreen(message: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(message)
            }
        }
    }
    @Composable
    private fun RequestPermissionScreen(onRequestPermission: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "需要蓝牙权限",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "本应用需要蓝牙权限来搜索和连接打印机。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onRequestPermission) {
                    Text("授予权限")
                }
            }
        }
    }
    @Composable
    private fun PermissionDeniedScreen(
        onOpenSettings: () -> Unit,
        onRetry: () -> Unit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "权限被拒绝",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "没有蓝牙权限，应用无法搜索和连接打印机。请在设置中手动开启权限。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onOpenSettings) {
                    Text("去设置开启")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
    }
    private fun requestPermissions(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        val permissions = getRequiredPermissions().toTypedArray()
        permissionLauncher.launch(permissions)
    }
    private fun checkBluetoothPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    private enum class PermissionState {
        CHECKING,
        REQUESTING,
        DENIED,
        GRANTED
    }
}
