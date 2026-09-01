package com.kuronoa.expensetracker.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuronoa.expensetracker.BuildConfig
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.R
import com.kuronoa.expensetracker.util.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: KuronoaApp) {
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory(app) { SettingsViewModel.factory(it) })
    val settings by viewModel.settings.collectAsState()
    val testState by viewModel.testState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var baseUrl by remember(settings.apiBaseUrl) { mutableStateOf(settings.apiBaseUrl) }
    var token by remember(settings.apiToken) { mutableStateOf(settings.apiToken) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_kuronoa),
                contentDescription = "Logo Kuronoa Bakery",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(90.dp)
            )

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Koneksi Google Sheets", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tempel URL Web App & Token dari Apps Script (lihat docs/PANDUAN_RILIS.md).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("URL Web App Apps Script") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("API Token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.testConnection(baseUrl, token) }, enabled = !testState.inProgress) {
                            if (testState.inProgress) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            }
                            Text("Uji Koneksi")
                        }
                        Button(onClick = { viewModel.saveConnection(baseUrl, token) }) {
                            Text("Simpan")
                        }
                    }
                    testState.resultMessage?.let {
                        Text(
                            it,
                            color = if (testState.success == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sinkronisasi", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sinkron otomatis latar belakang (~30 menit sekali)")
                        Switch(checked = settings.autoSyncEnabled, onCheckedChange = { viewModel.setAutoSync(it) })
                    }
                    Text(
                        "Sinkron terakhir: ${settings.lastSyncAt ?: "belum pernah"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { viewModel.syncNow() }, enabled = !syncState.inProgress) {
                        if (syncState.inProgress) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text("Sync Sekarang")
                    }
                    syncState.resultMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            HorizontalDivider()

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tentang Aplikasi", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(BuildConfig.APP_CREDIT, style = MaterialTheme.typography.bodyMedium)
                    Text("Data tersimpan di Google Sheets milik Anda sendiri.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
