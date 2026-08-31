package com.kuronoa.expensetracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.R
import com.kuronoa.expensetracker.core.logic.CurrencyFormatter
import com.kuronoa.expensetracker.ui.theme.CategoryPalette
import com.kuronoa.expensetracker.util.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(app: KuronoaApp) {
    val viewModel: DashboardViewModel = viewModel(factory = AppViewModelFactory(app) { DashboardViewModel.factory(it) })
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.syncMessage) {
        state.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_dashboard)) },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (state.isConfigured) Icons.Filled.Sync else Icons.Filled.CloudOff,
                                contentDescription = "Sync sekarang"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { padding ->
        val summary = state.summary
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ConnectionBanner(configured = state.isConfigured, lastSyncAt = state.lastSyncAt) }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Total Pengeluaran", color = MaterialTheme.colorScheme.onPrimary)
                        Text(
                            CurrencyFormatter.format(summary.grandTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "${summary.transactionCount} transaksi tercatat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (summary.byCategory.isNotEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Berdasarkan Kategori", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(
                                    slices = summary.byCategory.map { it.kategori to it.total },
                                    modifier = Modifier.size(120.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    summary.byCategory.take(6).forEachIndexed { index, cat ->
                                        LegendRow(
                                            color = CategoryPalette[index % CategoryPalette.size],
                                            label = cat.kategori,
                                            value = "${CurrencyFormatter.format(cat.total)} (${"%.0f".format(cat.percent)}%)"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (summary.byMonth.isNotEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Tren Bulanan", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            HorizontalBarChart(
                                items = summary.byMonth.map { it.bulan to it.total },
                                valueFormatter = { CurrencyFormatter.format(it) }
                            )
                        }
                    }
                }
            }

            if (summary.bySupplier.isNotEmpty()) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("Top Supplier", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            summary.bySupplier.forEach { (name, total) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium)
                                    Text(CurrencyFormatter.format(total), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ConnectionBanner(configured: Boolean, lastSyncAt: String?) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (configured) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (configured) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (configured) "Tersambung ke Google Sheets. Sinkron terakhir: ${lastSyncAt ?: "belum pernah"}"
                else "Belum terhubung ke Google Sheets — buka tab Pengaturan untuk memasukkan URL & token.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
