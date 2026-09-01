package com.kuronoa.expensetracker.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kuronoa.expensetracker.core.logic.CurrencyFormatter
import com.kuronoa.expensetracker.core.logic.ExpenseValidator
import com.kuronoa.expensetracker.core.model.ExpenseItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormContent(
    initial: ExpenseItem,
    errorMessage: String?,
    onSave: (ExpenseItem) -> Unit,
    onCancel: () -> Unit
) {
    var tanggal by remember { mutableStateOf(initial.tanggal.ifBlank { ExpenseViewModel.todayIso() }) }
    var kategori by remember { mutableStateOf(initial.kategori) }
    var uraian by remember { mutableStateOf(initial.uraian) }
    var lokasi by remember { mutableStateOf(initial.lokasi.ifBlank { ExpenseItem.LOKASI_OPTIONS.first() }) }
    var supplier by remember { mutableStateOf(initial.supplier) }
    var buktiTransaksi by remember { mutableStateOf(initial.buktiTransaksi) }
    var pembayaran by remember { mutableStateOf(initial.pembayaran.ifBlank { ExpenseItem.PEMBAYARAN_OPTIONS.first() }) }
    var jumlahText by remember { mutableStateOf(if (initial.jumlah > 0) initial.jumlah.toLong().toString() else "") }
    var nilaiTransferText by remember {
        mutableStateOf(initial.nilaiTransfer?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var keterangan by remember { mutableStateOf(initial.keterangan) }

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (initial.localId > 0) "Ubah Pengeluaran" else "Tambah Pengeluaran",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = tanggal,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tanggal") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Pilih tanggal")
                }
            },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
        )

        Dropdown(
            label = "Kategori",
            options = ExpenseItem.KATEGORI_OPTIONS,
            selected = kategori,
            onSelected = { kategori = it }
        )

        OutlinedTextField(
            value = nilaiTransferText,
            onValueChange = { input -> nilaiTransferText = input.filter { it.isDigit() } },
            label = { Text("Nilai Transfer (Rp, opsional)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                if (nilaiTransferText.isNotEmpty()) {
                    IconButton(onClick = { nilaiTransferText = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Hapus nilai transfer")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uraian,
            onValueChange = { uraian = it },
            label = { Text("Uraian / nama belanja") },
            modifier = Modifier.fillMaxWidth()
        )

        Dropdown(
            label = "Lokasi",
            options = ExpenseItem.LOKASI_OPTIONS,
            selected = lokasi,
            onSelected = { lokasi = it }
        )

        OutlinedTextField(
            value = supplier,
            onValueChange = { supplier = it },
            label = { Text("Supplier") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = buktiTransaksi,
            onValueChange = { buktiTransaksi = it },
            label = { Text("Bukti Transaksi (Online/Nota/Struk)") },
            modifier = Modifier.fillMaxWidth()
        )

        Dropdown(
            label = "Pembayaran",
            options = ExpenseItem.PEMBAYARAN_OPTIONS,
            selected = pembayaran,
            onSelected = { pembayaran = it }
        )

        OutlinedTextField(
            value = jumlahText,
            onValueChange = { input -> jumlahText = input.filter { it.isDigit() } },
            label = { Text("Jumlah (Rp)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = keterangan,
            onValueChange = { keterangan = it },
            label = { Text("Keterangan (opsional)") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Batal") }
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            tanggal = tanggal,
                            kategori = kategori,
                            nilaiTransfer = nilaiTransferText.let { CurrencyFormatter.parse(it) }.takeIf { it > 0 },
                            uraian = uraian.trim(),
                            lokasi = lokasi,
                            supplier = supplier.trim(),
                            buktiTransaksi = buktiTransaksi.trim(),
                            pembayaran = pembayaran,
                            jumlah = CurrencyFormatter.parse(jumlahText),
                            keterangan = keterangan.trim(),
                            bulan = ExpenseValidator.monthNameFromIsoDate(tanggal)
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("Simpan") }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoToMillis(tanggal))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { tanggal = millisToIso(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}

private fun isoToMillis(iso: String): Long? {
    if (!ExpenseValidator.isIsoDate(iso)) return System.currentTimeMillis()
    return runCatching {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        fmt.parse(iso)?.time
    }.getOrNull()
}

private fun millisToIso(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date(millis))
}
