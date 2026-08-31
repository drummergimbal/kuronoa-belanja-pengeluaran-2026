package com.kuronoa.expensetracker.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuronoa.expensetracker.KuronoaApp
import com.kuronoa.expensetracker.R
import com.kuronoa.expensetracker.core.logic.CurrencyFormatter
import com.kuronoa.expensetracker.core.model.ExpenseItem
import com.kuronoa.expensetracker.util.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(app: KuronoaApp) {
    val viewModel: ExpenseViewModel = viewModel(factory = AppViewModelFactory(app) { ExpenseViewModel.factory(it) })
    val expenses by viewModel.expenses.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val formError by viewModel.formError.collectAsState()

    var editingItem by remember { mutableStateOf<ExpenseItem?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<ExpenseItem?>(null) }

    val monthTotal = expenses.filter { !it.pendingDelete }.sumOf { it.jumlah }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_expenses)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingItem = ExpenseItem(bulan = selectedMonth)
                showForm = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthSelector(
                months = viewModel.months,
                selected = selectedMonth,
                onSelected = { viewModel.selectMonth(it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total $selectedMonth", style = MaterialTheme.typography.bodyMedium)
                Text(
                    CurrencyFormatter.format(monthTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada pengeluaran di bulan $selectedMonth.\nTekan tombol + untuk menambah.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.localId }) { item ->
                        ExpenseCard(
                            item = item,
                            onClick = { editingItem = item; showForm = true },
                            onDelete = { deleteCandidate = item }
                        )
                    }
                }
            }
        }
    }

    if (showForm && editingItem != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showForm = false; viewModel.clearFormError() },
            sheetState = sheetState
        ) {
            ExpenseFormContent(
                initial = editingItem!!,
                errorMessage = formError,
                onSave = { item ->
                    val ok = viewModel.save(item)
                    if (ok) showForm = false
                },
                onCancel = { showForm = false; viewModel.clearFormError() }
            )
        }
    }

    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Hapus pengeluaran?") },
            text = { Text("\"${item.uraian}\" sebesar ${CurrencyFormatter.format(item.jumlah)} akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    deleteCandidate = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun ExpenseCard(item: ExpenseItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.uraian.ifBlank { "(tanpa uraian)" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${item.kategori} • ${item.tanggal} • ${item.supplier.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.dirty || item.pendingDelete) {
                    Text(
                        if (item.pendingDelete) "Menunggu dihapus dari server…" else "Belum tersinkron…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.format(item.jumlah), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthSelector(months: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Bulan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { m ->
                DropdownMenuItem(text = { Text(m) }, onClick = { onSelected(m); expanded = false })
            }
        }
    }
}
