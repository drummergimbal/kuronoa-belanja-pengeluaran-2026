# Panduan Rilis — Kuronoa Expense Tracker

Aplikasi Android untuk mencatat pengeluaran belanja Kuronoa Bakery, tersinkron
dua arah dengan spreadsheet **"Pengeluaran Belanja 2026"** milik Anda di Google
Sheets. Dibuat oleh **drummergimbal**.

Panduan ini menuntun Anda dari nol sampai punya file `.apk` yang bisa
diinstal ke HP Android — tanpa perlu paham coding. Ikuti urutan langkahnya.

---

## Ringkasan cara kerja

```
[ HP Android ]  <-- sinkron 2 arah -->  [ Apps Script Web App ]  <-->  [ Google Sheets Anda ]
   (app ini)                              (jembatan API gratis)         (Pengeluaran Belanja 2026)
```

- Semua data **tetap tersimpan di Google Sheets Anda sendiri** — aplikasi
  tidak menyimpan data di server pihak ketiga manapun.
- Jembatan API (Apps Script Web App) dibuat & dimiliki oleh akun Google Anda
  sendiri, gratis, tidak perlu kartu kredit / hosting berbayar.
- Ada 3 langkah persiapan sekali-jalan: (1) deploy Apps Script, (2) build APK
  lewat GitHub Actions, (3) masukkan URL & token ke Settings di app.

---

## Langkah 1 — Deploy jembatan Apps Script (±10 menit, sekali saja)

1. Buka spreadsheet **Pengeluaran Belanja 2026** di akun Google Anda.
2. Klik menu **Extensions (Ekstensi) → Apps Script**. Tab baru akan terbuka
   berisi editor kode.
3. Hapus semua isi file `Code.gs` yang sudah ada (klik di area kode, `Ctrl+A`
   lalu `Delete`).
4. Buka file [`apps-script/Code.gs`](../apps-script/Code.gs) dari paket ini,
   salin **seluruh isinya**, lalu tempel ke editor Apps Script tadi.
5. Klik ikon **Save** (gambar disket) di toolbar Apps Script.
6. Di dropdown pemilih fungsi (sebelah tombol "Run" ▷ berbentuk segitiga),
   pilih fungsi **`setup`**, lalu klik **Run**.
   - Google akan minta izin akses ke spreadsheet Anda — klik **Review
     permissions**, pilih akun Anda, klik **Advanced** → **Go to (nama
     project) (unsafe)** → **Allow**. Ini normal karena scriptnya milik Anda
     sendiri (bukan pihak ketiga), Google hanya menampilkan peringatan standar
     untuk script yang belum di-publish ke Marketplace.
7. Setelah selesai jalan, buka menu **View → Logs** (atau tekan `Ctrl+Enter`).
   Anda akan melihat teks seperti ini — **catat / salin API TOKEN-nya**:
   ```
   ================ KURONOA SYNC — SETUP SELESAI ================
   API TOKEN (salin ke Settings app Android):
   a1b2c3d4e5f6...
   Sheet bulan terdeteksi: Januari, Februari, Maret, ...
   ================================================================
   ```
8. Klik tombol **Deploy → New deployment**.
   - Klik ikon gerigi ⚙️ di samping "Select type", pilih **Web app**.
   - Description: bebas, misal "Kuronoa Sync v1".
   - **Execute as**: `Me (email Anda)`.
   - **Who has access**: `Anyone`.
   - Klik **Deploy**.
   - Google akan minta konfirmasi izin sekali lagi — Allow.
9. Salin **Web app URL** yang muncul (bentuknya
   `https://script.google.com/macros/s/AKfycb.../exec`). Simpan bersama API
   TOKEN dari langkah 7 — dua-duanya akan dimasukkan ke app Android nanti.

> **Setiap kali Anda mengedit ulang `Code.gs`** (misal update dari versi
> baru), Anda wajib buat deployment baru: **Deploy → Manage deployments →
> pensil (Edit) → Version: New version → Deploy**. URL-nya tetap sama.

---

## Langkah 2 — Build APK lewat GitHub Actions (±10 menit, sekali saja)

Kenapa lewat GitHub, bukan langsung dari sini? Proses compile APK Android
butuh Android SDK resmi dari Google yang hanya bisa diakses dari lingkungan
yang punya akses internet penuh (seperti server GitHub) — bukan dari
lingkungan sandbox tempat kode ini ditulis. GitHub Actions **gratis** untuk
repository publik/pribadi skala kecil seperti ini, dan akan otomatis
meng-compile APK setiap kali kode di-push.

1. Buat repository baru di [github.com](https://github.com/new) (boleh
   Private), misal namanya `kuronoa-expense-tracker`.
2. Upload **seluruh isi folder proyek ini** ke repository tersebut. Cara
   termudah lewat browser:
   - Di halaman repo GitHub yang baru, klik **uploading an existing file**.
   - Seret (drag & drop) semua file & folder dari paket ini ke sana.
   - Klik **Commit changes**.
   
   Atau lewat command line (jika Anda familiar dengan git):
   ```bash
   cd kuronoa-expense-tracker
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/USERNAME/kuronoa-expense-tracker.git
   git push -u origin main
   ```
3. Buka tab **Actions** di repository GitHub Anda. Workflow **"Build &
   Release APK"** akan otomatis berjalan (butuh beberapa menit — ada proses
   testing otomatis lalu compile).
4. Setelah selesai (tanda centang hijau ✅), klik workflow run tersebut, lalu
   scroll ke bagian **Artifacts** di bawah. Unduh **`kuronoa-expense-tracker-debug`**
   (file `.zip` berisi `.apk` — pakai ini untuk uji coba cepat) — atau
   **`kuronoa-expense-tracker-release`** untuk versi yang sudah dioptimasi
   (lebih kecil ukurannya, disarankan untuk pemakaian sehari-hari).
5. Ekstrak file zip tersebut untuk mendapatkan file `.apk`.

### (Opsional) Rilis resmi dengan versi & changelog

Jika Anda push sebuah **tag** berformat `v1.0.0` (misalnya lewat `git tag
v1.0.0 && git push origin v1.0.0`, atau lewat menu **Releases → Draft a new
release** di GitHub dan membuat tag baru), workflow akan otomatis membuat
**GitHub Release** berisi file APK yang siap dibagikan via link — jadi Anda
(atau tim) tinggal buka halaman Releases untuk unduh versi terbaru kapan saja.

### (Opsional) Tanda tangan APK konsisten antar-update

Tanpa langkah ini, APK tetap bisa diinstal & dipakai normal (ditandatangani
otomatis pakai kunci debug bawaan). Tapi supaya **update APK di HP tidak perlu
uninstall dulu** (Android menolak update jika tanda tangan berbeda), buat
keystore sendiri:

```bash
keytool -genkey -v -keystore release.keystore -alias kuronoa \
  -keyalg RSA -keysize 2048 -validity 10000
```

Lalu di GitHub: **Settings → Secrets and variables → Actions → New repository
secret**, tambahkan 4 secret:
- `KEYSTORE_BASE64` — isi dengan hasil `base64 -w0 release.keystore` (satu baris panjang)
- `KEYSTORE_PASSWORD` — password keystore yang Anda buat
- `KEY_ALIAS` — `kuronoa` (atau alias yang Anda pilih)
- `KEY_PASSWORD` — password key (biasanya sama dengan KEYSTORE_PASSWORD)

Build berikutnya otomatis pakai keystore ini. **Simpan file `release.keystore`
baik-baik** (di luar repo!) — kalau hilang, Anda tidak akan bisa
mengupdate APK yang sudah beredar dan harus rilis sebagai app baru.

---

## Langkah 3 — Install APK ke HP Android

1. Pindahkan file `.apk` ke HP (lewat kabel USB, Google Drive, WhatsApp ke
   diri sendiri, dll).
2. Buka file `.apk` tersebut dari File Manager / notifikasi unduhan.
3. Jika muncul peringatan "Install dari sumber tidak dikenal", ikuti
   petunjuk di layar untuk mengizinkannya (Android akan mengarahkan ke
   Settings, aktifkan untuk aplikasi yang Anda pakai membuka file, lalu
   kembali dan lanjutkan instal). Ini normal untuk APK yang tidak dari Play
   Store.
4. Tunggu proses instal selesai, buka aplikasi **Kuronoa Pengeluaran**.

---

## Langkah 4 — Sambungkan aplikasi ke Google Sheets

1. Buka app, masuk ke tab **Pengaturan**.
2. Tempel **Web app URL** dan **API Token** dari Langkah 1.
3. Tekan **Uji Koneksi** — harus muncul "Berhasil terhubung!".
4. Tekan **Simpan**, lalu tekan **Sync Sekarang**.
5. Buka tab **Pengeluaran** — data dari spreadsheet Anda akan muncul di app.
   Coba tambah satu transaksi baru di app, lalu cek spreadsheet — barisnya
   akan muncul otomatis dalam beberapa detik.

Sinkronisasi berjalan otomatis di latar belakang tiap ±30 menit selama ada
koneksi internet, dan juga otomatis setiap kali Anda menambah/mengubah/
menghapus data di app. Tombol "Sync Sekarang" bisa dipakai kapan saja untuk
memaksa sinkron seketika (misalnya sebelum membuka Google Sheets di
komputer untuk memastikan datanya paling baru).

---

## Struktur data di spreadsheet

Aplikasi membaca/menulis sheet bulanan yang sudah ada (Januari–Desember
2026) memakai kolom yang sama seperti yang sudah Anda pakai (NO, TANGGAL,
BULAN, KATEGORI, NILAI TRANSFER, URAIAN, LOKASI, SUPPLIER, BUKTI TRANSAKSI,
PEMBAYARAN, JUMLAH, dst). Setup menambahkan **2 kolom tersembunyi baru** di
sebelah kanan (kolom `ID` & `UPDATED_AT`) yang dipakai sistem untuk melacak
sinkronisasi — jangan dihapus/diedit manual, tapi aman untuk dibiarkan
(tidak mengganggu formula/rekap yang sudah ada).

---

## Uji yang sudah dilakukan sebelum rilis

- **37 unit test** untuk logika inti (format Rupiah, validasi form,
  penggabungan data sinkron 2-arah, ringkasan dashboard) — semuanya lulus,
  bisa dijalankan ulang dengan `bash scripts/run_core_tests.sh` (tidak perlu
  Android Studio, hanya perlu Java).
- Validasi sintaks `apps-script/Code.gs` dan seluruh file konfigurasi XML.
- Tinjauan manual menyeluruh atas seluruh source code Kotlin.
- **Test otomatis lengkap + compile APK sungguhan** dijalankan oleh GitHub
  Actions setiap kali kode di-push (lihat tab Actions di repo Anda) — ini
  jadi lapisan uji akhir sebelum APK dianggap siap pakai, karena
  memverifikasi build di lingkungan Android SDK yang sesungguhnya.

## Troubleshooting

| Masalah | Solusi |
|---|---|
| "Uji Koneksi" gagal / timeout | Pastikan Web app URL benar (diakhiri `/exec`), dan deployment "Who has access" = Anyone |
| Data tidak muncul setelah Sync | Cek nama bulan di app cocok dgn nama tab sheet (Januari, Februari, ...); tekan Sync Sekarang lagi |
| Muncul "Token tidak valid" | Token salah salin / ada spasi tambahan — copy ulang dari Logs Apps Script |
| GitHub Actions gagal (merah ❌) | Klik run yang gagal, buka log step yg merah untuk detail errornya, atau minta bantuan lagi dengan menempel pesan errornya |
| Update APK gagal terinstal ("App not installed") | HP masih punya versi lama dgn tanda tangan berbeda — uninstall dulu versi lama, atau pakai keystore sendiri (lihat bagian opsional di atas) supaya update berikutnya lancar |

---

_Kuronoa Expense Tracker — by drummergimbal_
