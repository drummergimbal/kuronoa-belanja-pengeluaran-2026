# Kuronoa Expense Tracker

Aplikasi Android untuk mencatat pengeluaran belanja dapur produksi & toko
Kuronoa Bakery — tambah/ubah/hapus data, dashboard ringkasan, dan
**sinkronisasi dua arah** dengan spreadsheet Google Sheets **"Pengeluaran
Belanja 2026"**.

**📖 Mulai dari sini:** [`docs/PANDUAN_RILIS.md`](docs/PANDUAN_RILIS.md) —
panduan langkah demi langkah dari nol sampai APK terinstal di HP, tanpa perlu
paham coding.

## Fitur

- **Input, edit, hapus** transaksi pengeluaran (termasuk kolom **Nilai Transfer**), langsung tersimpan offline.
- **Ekspor ke PDF**: laporan pengeluaran per bulan (tabel + total) bisa diekspor jadi file PDF langsung dari tab Pengeluaran, lalu dibagikan/disimpan lewat aplikasi lain (WhatsApp, Drive, viewer PDF, dll).
- **Sinkronisasi 2 arah** ke Google Sheets lewat jembatan Apps Script Web App
  (data tetap 100% milik Anda, tersimpan di Google Sheets Anda sendiri).
- **Koneksi stabil**: retry otomatis dgn exponential backoff di level HTTP
  maupun di level background worker, antrian perubahan offline-first (input
  tidak pernah menunggu jaringan), penguncian (LockService) di sisi server
  agar sinkron dari beberapa HP tidak saling tabrakan.
- **Dashboard**: total pengeluaran, breakdown per kategori (donut chart),
  tren bulanan (bar chart), top supplier.
- Ikon aplikasi & branding memakai logo Kuronoa Bakery.
- Versi & kredit developer (`by drummergimbal`) ditampilkan di tab Pengaturan.

## Struktur proyek

```
kuronoa-expense-tracker/
├── app/                    Aplikasi Android (Kotlin + Jetpack Compose)
├── core/                   Logika bisnis murni (Kotlin/JVM, unit-testable tanpa Android)
├── apps-script/Code.gs     Jembatan API (Google Apps Script Web App)
├── docs/PANDUAN_RILIS.md   Panduan lengkap: deploy, build APK, install, troubleshooting
├── icons/                  Aset ikon sumber (hasil olahan logo Kuronoa)
├── scripts/                Skrip bantu (menjalankan unit test :core secara offline)
└── .github/workflows/      CI: test otomatis + build APK + rilis GitHub Release
```

## Arsitektur singkat

- **`:core`** — model data (`ExpenseItem`), logika sinkronisasi
  (`SyncMerger`: push/pull/merge dgn strategi last-write-wins), validasi
  form, agregasi dashboard. Tidak bergantung pada Android SDK sama sekali,
  jadi bisa di-unit-test dalam hitungan detik.
- **`:app`** — Room (database lokal/offline), OkHttp+Moshi (klien API ke
  Apps Script), WorkManager (sinkron latar belakang tiap 30 menit + retry),
  Jetpack Compose (UI: Dashboard, Pengeluaran, Pengaturan).
- **`apps-script/Code.gs`** — API JSON (`action=list/create/update/delete/
  batchSync/recap/months/ping`) yang jalan di atas spreadsheet Anda sendiri,
  dgn token auth sederhana & penguncian transaksi.

## Menjalankan unit test tanpa Android Studio

```bash
bash scripts/run_core_tests.sh
```

Meng-compile & menjalankan 37 unit test modul `:core` (logika sync, validasi,
format Rupiah, dashboard) hanya dgn Java — tidak perlu Android SDK/Gradle
penuh. Berguna untuk verifikasi cepat sebelum push.

## Build APK

Lihat [`docs/PANDUAN_RILIS.md`](docs/PANDUAN_RILIS.md) bagian "Langkah 2" —
cukup push kode ini ke GitHub, APK ter-compile otomatis lewat GitHub Actions
(gratis) dan bisa diunduh dari tab **Actions** atau **Releases**.

Jika Anda punya Android Studio + Android SDK terpasang, proyek ini juga bisa
langsung dibuka & di-build secara normal (`Open an existing project` →
pilih folder ini → biarkan Android Studio membuat `gradlew` otomatis lewat
**File → Sync Project with Gradle Files**).

---

_Kuronoa Expense Tracker v1.1.0 — by drummergimbal_
