/**
 * Kuronoa Bakery — Pengeluaran Belanja — Sync Bridge (Apps Script Web App)
 * ---------------------------------------------------------------------
 * Jembatan API JSON dua-arah antara aplikasi Android "Kuronoa Expense
 * Tracker" dan spreadsheet "Pengeluaran Belanja 2026".
 *
 * Dibuat oleh: drummergimbal
 *
 * CARA PASANG (sekali saja):
 * 1. Buka spreadsheet "Pengeluaran Belanja 2026" di akun Google Anda.
 * 2. Menu Extensions > Apps Script.
 * 3. Hapus isi Code.gs bawaan, tempel seluruh isi file ini.
 * 4. Jalankan fungsi `setup` sekali (pilih function "setup" di dropdown
 *    toolbar Apps Script lalu klik Run). Izinkan akses saat diminta.
 * 5. Buka tab "Executions" atau "Logs" (Ctrl+Enter setelah run) untuk
 *    menyalin API TOKEN yang tercetak — token ini dipakai di app Android.
 * 6. Deploy > New deployment > pilih tipe "Web app".
 *    - Execute as: Me
 *    - Who has access: Anyone
 *    Klik Deploy, salin "Web app URL" — itu dipakai di app Android.
 * 7. Setiap kali Anda mengubah script ini, buat deployment baru
 *    (Deploy > Manage deployments > Edit > New version) supaya URL yang
 *    sama memakai kode terbaru.
 *
 * Detail lengkap ada di docs/PANDUAN_RILIS.md.
 */

// ====== KONFIGURASI ======
var RECAP_SHEET_NAME = 'Rekap 2026';
var HEADER_ROW = 7;      // baris judul kolom (NO, TANGGAL, ...)
var DATA_START_ROW = 8;  // baris pertama data transaksi
var ID_COL = 20;         // kolom T -> ID unik (dibuat otomatis)
var UPDATED_AT_COL = 21; // kolom U -> waktu update terakhir (dibuat otomatis)
var TOKEN_PROP_KEY = 'KURONOA_API_TOKEN';

var MONTH_NAMES = ['Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
  'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'];

// Nama field JSON <-> nomor kolom (1-indexed) pada setiap sheet bulan.
var FIELD_COLS = {
  no: 1,
  tanggal: 2,
  bulan: 3,
  kategori: 4,
  nilaiTransfer: 5,
  uraian: 6,
  lokasi: 7,
  supplier: 8,
  buktiTransaksi: 9,
  pembayaran: 10,
  jumlah: 11,
  tanggalPembayaran: 12,
  noPV: 13,
  keterangan: 14,
  cekTransfer: 15
};

// ====== SETUP (dijalankan manual sekali oleh pemilik sheet) ======
function setup() {
  var props = PropertiesService.getScriptProperties();
  var token = props.getProperty(TOKEN_PROP_KEY);
  if (!token) {
    token = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '').substring(0, 8);
    props.setProperty(TOKEN_PROP_KEY, token);
  }
  var months = listMonthSheets_();
  for (var i = 0; i < months.length; i++) {
    ensureIdColumns_(SpreadsheetApp.getActive().getSheetByName(months[i]));
  }
  Logger.log('================ KURONOA SYNC — SETUP SELESAI ================');
  Logger.log('API TOKEN (salin ke Settings app Android):');
  Logger.log(token);
  Logger.log('Sheet bulan terdeteksi: ' + months.join(', '));
  Logger.log('================================================================');
  return token;
}

// ====== ENTRY POINTS WEB APP ======
function doGet(e) {
  return handleRequest_(e, 'GET');
}

function doPost(e) {
  return handleRequest_(e, 'POST');
}

function handleRequest_(e, method) {
  var lock = LockService.getScriptLock();
  var lockAcquired = false;
  try {
    var params = method === 'GET' ? (e.parameter || {}) : parseBody_(e);
    var action = params.action || 'ping';

    if (action !== 'ping') {
      if (!checkToken_(params.token)) {
        return jsonOut_({ ok: false, error: 'UNAUTHORIZED', message: 'Token tidak valid.' });
      }
    }

    lockAcquired = lock.tryLock(10000);
    if (!lockAcquired && action !== 'ping') {
      return jsonOut_({ ok: false, error: 'BUSY', message: 'Server sedang sibuk, coba lagi.' });
    }

    switch (action) {
      case 'ping':
        return jsonOut_({ ok: true, action: 'ping', serverTime: nowIso_(), version: '1.0' });
      case 'months':
        return jsonOut_({ ok: true, months: listMonthSheets_() });
      case 'list':
        return jsonOut_(actionList_(params.month, params.since));
      case 'all':
        return jsonOut_(actionAll_(params.since));
      case 'recap':
        return jsonOut_(actionRecap_());
      case 'create':
        return jsonOut_(actionCreate_(params.month, params.item));
      case 'update':
        return jsonOut_(actionUpdate_(params.month, params.item));
      case 'delete':
        return jsonOut_(actionDelete_(params.month, params.id));
      case 'batchSync':
        return jsonOut_(actionBatchSync_(params.operations, params.since));
      default:
        return jsonOut_({ ok: false, error: 'UNKNOWN_ACTION', message: 'Aksi tidak dikenal: ' + action });
    }
  } catch (err) {
    return jsonOut_({ ok: false, error: 'SERVER_ERROR', message: String(err && err.message ? err.message : err) });
  } finally {
    if (lockAcquired) lock.releaseLock();
  }
}

// ====== ACTIONS ======
function actionList_(month, since) {
  var sheet = getMonthSheet_(month);
  ensureIdColumns_(sheet);
  var items = readRows_(sheet, since);
  return { ok: true, month: month, serverTime: nowIso_(), items: items };
}

function actionAll_(since) {
  var months = listMonthSheets_();
  var out = {};
  for (var i = 0; i < months.length; i++) {
    var sheet = SpreadsheetApp.getActive().getSheetByName(months[i]);
    ensureIdColumns_(sheet);
    out[months[i]] = readRows_(sheet, since);
  }
  return { ok: true, serverTime: nowIso_(), months: out };
}

function actionRecap_() {
  var months = listMonthSheets_();
  var byMonth = [];
  var categoryTotals = {};
  var grandTotal = 0;
  for (var i = 0; i < months.length; i++) {
    var sheet = SpreadsheetApp.getActive().getSheetByName(months[i]);
    var rows = readRows_(sheet, null);
    var monthTotal = 0;
    for (var j = 0; j < rows.length; j++) {
      var jumlah = Number(rows[j].jumlah) || 0;
      monthTotal += jumlah;
      var kat = rows[j].kategori || 'Lainnya';
      categoryTotals[kat] = (categoryTotals[kat] || 0) + jumlah;
    }
    byMonth.push({ month: months[i], total: monthTotal, count: rows.length });
    grandTotal += monthTotal;
  }
  var byCategory = [];
  for (var kat2 in categoryTotals) {
    byCategory.push({ kategori: kat2, total: categoryTotals[kat2] });
  }
  byCategory.sort(function (a, b) { return b.total - a.total; });
  return { ok: true, serverTime: nowIso_(), byMonth: byMonth, byCategory: byCategory, grandTotal: grandTotal };
}

function actionCreate_(month, item) {
  if (!item) return { ok: false, error: 'BAD_REQUEST', message: 'item wajib diisi.' };
  var sheet = getMonthSheet_(month);
  ensureIdColumns_(sheet);
  var row = appendRow_(sheet, item);
  return { ok: true, item: row };
}

function actionUpdate_(month, item) {
  if (!item || !item.id) return { ok: false, error: 'BAD_REQUEST', message: 'item.id wajib diisi.' };
  var sheet = getMonthSheet_(month);
  ensureIdColumns_(sheet);
  var result = updateRowById_(sheet, item);
  if (!result) return { ok: false, error: 'NOT_FOUND', message: 'Baris dengan id tsb tidak ditemukan (mungkin sudah dihapus).' };
  return { ok: true, item: result };
}

function actionDelete_(month, id) {
  if (!id) return { ok: false, error: 'BAD_REQUEST', message: 'id wajib diisi.' };
  var sheet = getMonthSheet_(month);
  ensureIdColumns_(sheet);
  var deleted = deleteRowById_(sheet, id);
  if (!deleted) return { ok: false, error: 'NOT_FOUND', message: 'Baris sudah tidak ada di server.' };
  return { ok: true, id: id };
}

// Push semua perubahan lokal + tarik perubahan server sejak `since`, satu kali round-trip.
function actionBatchSync_(operations, since) {
  var results = [];
  operations = operations || [];
  for (var i = 0; i < operations.length; i++) {
    var op = operations[i];
    var sheet = getMonthSheet_(op.month);
    ensureIdColumns_(sheet);
    var res;
    try {
      if (op.op === 'create') {
        res = { ok: true, clientTempId: op.clientTempId, item: appendRow_(sheet, op.item) };
      } else if (op.op === 'update') {
        var updated = updateRowById_(sheet, op.item);
        res = updated
          ? { ok: true, item: updated }
          : { ok: false, error: 'NOT_FOUND', id: op.item && op.item.id };
      } else if (op.op === 'delete') {
        var wasDeleted = deleteRowById_(sheet, op.id);
        res = { ok: wasDeleted, id: op.id, error: wasDeleted ? undefined : 'NOT_FOUND' };
      } else {
        res = { ok: false, error: 'UNKNOWN_OP' };
      }
    } catch (opErr) {
      res = { ok: false, error: 'OP_ERROR', message: String(opErr) };
    }
    res.op = op.op;
    res.month = op.month;
    results.push(res);
  }
  var pulled = actionAll_(since);
  return { ok: true, serverTime: nowIso_(), pushResults: results, months: pulled.months };
}

// ====== HELPERS: SHEET I/O ======
function listMonthSheets_() {
  var sheets = SpreadsheetApp.getActive().getSheets();
  var out = [];
  for (var i = 0; i < sheets.length; i++) {
    var name = sheets[i].getName();
    if (name === RECAP_SHEET_NAME) continue;
    for (var m = 0; m < MONTH_NAMES.length; m++) {
      if (name.toLowerCase().indexOf(MONTH_NAMES[m].toLowerCase()) === 0) {
        out.push(name);
        break;
      }
    }
  }
  return out;
}

function getMonthSheet_(month) {
  if (!month) throw new Error('Parameter month wajib diisi.');
  var sheet = SpreadsheetApp.getActive().getSheetByName(month);
  if (!sheet) throw new Error('Sheet bulan "' + month + '" tidak ditemukan.');
  return sheet;
}

function ensureIdColumns_(sheet) {
  var header = sheet.getRange(HEADER_ROW, ID_COL, 1, 2).getValues()[0];
  if (header[0] === 'ID' && header[1] === 'UPDATED_AT') return;
  sheet.getRange(HEADER_ROW, ID_COL, 1, 2).setValues([['ID', 'UPDATED_AT']]);
  var lastRow = sheet.getLastRow();
  if (lastRow < DATA_START_ROW) return;
  var n = lastRow - DATA_START_ROW + 1;
  var idRange = sheet.getRange(DATA_START_ROW, ID_COL, n, 2);
  var vals = idRange.getValues();
  var uraianVals = sheet.getRange(DATA_START_ROW, FIELD_COLS.uraian, n, 1).getValues();
  var now = nowIso_();
  var changed = false;
  for (var i = 0; i < n; i++) {
    var hasContent = String(uraianVals[i][0] || '').trim().length > 0;
    if (hasContent && !vals[i][0]) {
      vals[i][0] = Utilities.getUuid();
      vals[i][1] = now;
      changed = true;
    }
  }
  if (changed) idRange.setValues(vals);
}

function readRows_(sheet, since) {
  var lastRow = sheet.getLastRow();
  if (lastRow < DATA_START_ROW) return [];
  var n = lastRow - DATA_START_ROW + 1;
  var lastCol = Math.max(UPDATED_AT_COL, sheet.getLastColumn());
  var values = sheet.getRange(DATA_START_ROW, 1, n, lastCol).getValues();
  var sinceMs = since ? new Date(since).getTime() : null;
  var out = [];
  for (var i = 0; i < n; i++) {
    var row = values[i];
    var id = row[ID_COL - 1];
    var uraian = row[FIELD_COLS.uraian - 1];
    if (!id && !String(uraian || '').trim()) continue; // baris kosong
    var updatedAt = row[UPDATED_AT_COL - 1];
    if (sinceMs && updatedAt) {
      var rowMs = new Date(updatedAt).getTime();
      if (rowMs <= sinceMs) continue;
    }
    out.push(rowToItem_(row, id, updatedAt));
  }
  return out;
}

// Semua field bertipe string DIPAKSA jadi string, dan semua field angka
// DIPAKSA jadi number, supaya kontrak JSON stabil & tidak pernah membuat
// parser di sisi Android gagal karena tipe data tak terduga dari Sheets.
function rowToItem_(row, id, updatedAt) {
  return {
    id: id || '',
    no: str_(row[FIELD_COLS.no - 1]),
    tanggal: formatDate_(row[FIELD_COLS.tanggal - 1]),
    bulan: str_(row[FIELD_COLS.bulan - 1]),
    kategori: str_(row[FIELD_COLS.kategori - 1]),
    nilaiTransfer: numOrNull_(row[FIELD_COLS.nilaiTransfer - 1]),
    uraian: str_(row[FIELD_COLS.uraian - 1]),
    lokasi: str_(row[FIELD_COLS.lokasi - 1]),
    supplier: str_(row[FIELD_COLS.supplier - 1]),
    buktiTransaksi: str_(row[FIELD_COLS.buktiTransaksi - 1]),
    pembayaran: str_(row[FIELD_COLS.pembayaran - 1]),
    jumlah: num_(row[FIELD_COLS.jumlah - 1]),
    tanggalPembayaran: formatDate_(row[FIELD_COLS.tanggalPembayaran - 1]),
    noPV: str_(row[FIELD_COLS.noPV - 1]),
    keterangan: str_(row[FIELD_COLS.keterangan - 1]),
    cekTransfer: str_(row[FIELD_COLS.cekTransfer - 1]),
    updatedAt: updatedAt ? formatDateTime_(updatedAt) : null
  };
}

function str_(val) {
  if (val === null || val === undefined) return '';
  if (Object.prototype.toString.call(val) === '[object Date]') return formatDate_(val);
  return String(val);
}

function num_(val) {
  var n = Number(val);
  return isNaN(n) ? 0 : n;
}

function numOrNull_(val) {
  if (val === '' || val === null || val === undefined) return null;
  var n = Number(val);
  return isNaN(n) ? null : n;
}

function itemToRowArray_(item, existingRow) {
  var row = existingRow ? existingRow.slice() : new Array(15).fill('');
  function set(field, val) {
    if (val === undefined) return;
    row[FIELD_COLS[field] - 1] = val;
  }
  set('no', item.no);
  set('tanggal', item.tanggal ? parseDate_(item.tanggal) : undefined);
  set('bulan', item.bulan);
  set('kategori', item.kategori);
  set('nilaiTransfer', item.nilaiTransfer);
  set('uraian', item.uraian);
  set('lokasi', item.lokasi);
  set('supplier', item.supplier);
  set('buktiTransaksi', item.buktiTransaksi);
  set('pembayaran', item.pembayaran);
  set('jumlah', item.jumlah);
  set('tanggalPembayaran', item.tanggalPembayaran ? parseDate_(item.tanggalPembayaran) : undefined);
  set('noPV', item.noPV);
  set('keterangan', item.keterangan);
  set('cekTransfer', item.cekTransfer);
  return row;
}

function appendRow_(sheet, item) {
  var lastRow = Math.max(sheet.getLastRow(), DATA_START_ROW - 1);
  var targetRow = lastRow + 1;
  var id = Utilities.getUuid();
  var now = nowIso_();
  if (!item.no) {
    item.no = nextSequenceNo_(sheet);
  }
  var rowArray = itemToRowArray_(item, null);
  sheet.getRange(targetRow, 1, 1, rowArray.length).setValues([rowArray]);
  sheet.getRange(targetRow, ID_COL, 1, 2).setValues([[id, now]]);
  return rowToItem_(sheet.getRange(targetRow, 1, 1, UPDATED_AT_COL).getValues()[0], id, now);
}

function nextSequenceNo_(sheet) {
  var lastRow = sheet.getLastRow();
  if (lastRow < DATA_START_ROW) return 1;
  var n = lastRow - DATA_START_ROW + 1;
  var vals = sheet.getRange(DATA_START_ROW, FIELD_COLS.no, n, 1).getValues();
  var max = 0;
  for (var i = 0; i < n; i++) {
    var v = Number(vals[i][0]);
    if (!isNaN(v) && v > max) max = v;
  }
  return max + 1;
}

function findRowIndexById_(sheet, id) {
  var lastRow = sheet.getLastRow();
  if (lastRow < DATA_START_ROW) return -1;
  var n = lastRow - DATA_START_ROW + 1;
  var ids = sheet.getRange(DATA_START_ROW, ID_COL, n, 1).getValues();
  for (var i = 0; i < n; i++) {
    if (ids[i][0] === id) return DATA_START_ROW + i;
  }
  return -1;
}

function updateRowById_(sheet, item) {
  var rIdx = findRowIndexById_(sheet, item.id);
  if (rIdx === -1) return null;
  var existing = sheet.getRange(rIdx, 1, 1, 15).getValues()[0];
  var rowArray = itemToRowArray_(item, existing);
  var now = nowIso_();
  sheet.getRange(rIdx, 1, 1, rowArray.length).setValues([rowArray]);
  sheet.getRange(rIdx, UPDATED_AT_COL, 1, 1).setValue(now);
  return rowToItem_(sheet.getRange(rIdx, 1, 1, UPDATED_AT_COL).getValues()[0], item.id, now);
}

function deleteRowById_(sheet, id) {
  var rIdx = findRowIndexById_(sheet, id);
  if (rIdx === -1) return false;
  sheet.deleteRow(rIdx);
  return true;
}

// ====== HELPERS: MISC ======
function checkToken_(token) {
  var expected = PropertiesService.getScriptProperties().getProperty(TOKEN_PROP_KEY);
  return !!expected && token === expected;
}

function parseBody_(e) {
  if (!e || !e.postData || !e.postData.contents) return {};
  try {
    return JSON.parse(e.postData.contents);
  } catch (err) {
    return {};
  }
}

function jsonOut_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function nowIso_() {
  return Utilities.formatDate(new Date(), Session.getScriptTimeZone() || 'Asia/Jakarta', "yyyy-MM-dd'T'HH:mm:ss'Z'");
}

function formatDate_(val) {
  if (!val) return '';
  if (Object.prototype.toString.call(val) === '[object Date]') {
    return Utilities.formatDate(val, Session.getScriptTimeZone() || 'Asia/Jakarta', 'yyyy-MM-dd');
  }
  return String(val);
}

function formatDateTime_(val) {
  if (!val) return null;
  if (Object.prototype.toString.call(val) === '[object Date]') {
    return Utilities.formatDate(val, Session.getScriptTimeZone() || 'Asia/Jakarta', "yyyy-MM-dd'T'HH:mm:ss'Z'");
  }
  return String(val);
}

function parseDate_(val) {
  if (!val) return '';
  var d = new Date(val);
  return isNaN(d.getTime()) ? val : d;
}
