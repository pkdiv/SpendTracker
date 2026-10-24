package dev.pkdiv.spendtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.pkdiv.spendtracker.ui.SpendTrackerApp
import dev.pkdiv.spendtracker.ui.disclosure.DisclosureScreen
import dev.pkdiv.spendtracker.ui.disclosure.DisclosureViewModel
import dev.pkdiv.spendtracker.ui.disclosure.NotificationRationaleDialog
import dev.pkdiv.spendtracker.ui.theme.SpendTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val disclosureViewModel: DisclosureViewModel by viewModels()

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val anyGranted = granted.values.any { it }
        if (anyGranted) {
            mainViewModel.backfill()
        }
        disclosureViewModel.onSmsPermissionResult(anyGranted)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendTrackerTheme {
                val showDisclosure by disclosureViewModel.showDisclosure.collectAsStateWithLifecycle()
                val showNotificationRationale by
                    disclosureViewModel.showNotificationRationale.collectAsStateWithLifecycle()

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (showDisclosure) {
                        null -> Unit
                        true -> DisclosureScreen(
                            onAllowSms = {
                                disclosureViewModel.onSmsDisclosureAccepted()
                                requestSmsPermissions()
                            },
                            onContinueWithoutSms = { disclosureViewModel.onSmsDisclosureDeclined() },
                        )
                        false -> SpendTrackerApp()
                    }
                }

                if (showNotificationRationale) {
                    NotificationRationaleDialog(
                        onAllow = {
                            disclosureViewModel.onNotificationRationaleHandled()
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        },
                        onDismiss = { disclosureViewModel.onNotificationRationaleHandled() },
                    )
                }
            }
        }
        if (hasSmsPermission()) {
            mainViewModel.backfill()
        }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun requestSmsPermissions() {
        val permissions = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECEIVE_SMS)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_SMS)
            }
        }
        if (permissions.isNotEmpty()) {
            smsPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            mainViewModel.backfill()
            disclosureViewModel.onSmsPermissionResult(hasSmsPermission())
        }
    }
}
