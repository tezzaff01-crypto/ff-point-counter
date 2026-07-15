# Point Counter — Native (Kotlin + Jetpack Compose)

Ini adalah hasil rewrite penuh dari `POINT_COUNTER_1_0_downloadlistener__4_.apk` (yang aslinya
WebView + HTML/CSS/JS ±9.200 baris) menjadi **aplikasi Android native murni**: tanpa WebView,
tanpa aset HTML/JS, tanpa CDN (html2canvas/exceljs), tanpa izin INTERNET.

Package name (`com.pointcounter.tezzyruok`) dan nama app dipertahankan sama persis dengan APK asli
supaya bisa jadi update/pengganti langsung.

## Yang sudah di-porting 1:1

- **Setup**: nama turnamen, panitia, tahapan (Kualifikasi/Semifinal/Grand Final), jumlah sesi/match/tim,
  daftar tim per sesi, sistem poin custom (poin per rank + kill multiplier) — logic sama persis dengan
  `calcTotal()`, `getRankPts()`, `updateTeamCount()` di JS aslinya.
- **Match**: input kill + rank per tim per match, validasi rank tidak boleh dobel antar tim, auto-hitung total.
- **Leaderboard**: agregasi skor semua match dalam 1 sesi, sortable (Total/Kill/RankPts), dengan
  tie-breaker yang sama seperti `getLeaderboardRows()` di JS.
- **Most Kill**: catatan sampai 3 top-killer per tim per sesi.
- **Poster & Sertifikat**: digambar langsung pakai `android.graphics.Canvas` (padanan native dari
  html2canvas), lalu disimpan ke galeri lewat MediaStore.
- **Export Excel**: penulis `.xlsx` native minimal (zip + XML OOXML manual, tanpa library eksternal),
  padanan dari exceljs.
- **Penyimpanan data**: JSON di storage internal aplikasi (`StorageRepository`), padanan dari
  `localStorage`/`saveToStorage()`/`autoSave()` versi WebView — otomatis tersimpan setiap ada perubahan.
- **Reset**: hapus semua data (padanan `confirmResetAll()`).

## Yang disederhanakan / belum sepenuhnya identik

Karena ini rewrite total (bukan konversi otomatis), beberapa detail visual & fitur kecil dari versi
WebView (splash video, animasi CSS, custom-select dropdown, mode "CS Mode", upload logo turnamen ke
poster, bulk-input tim) belum semuanya ada — tapi **seluruh alur data & kalkulasi skor** sudah sama
persis, dan strukturnya sudah siap dikembangkan lebih lanjut (tinggal tambah screen/komponen baru).

## Cara build TANPA Android Studio (pakai GitHub Actions — gratis)

Project ini sudah dilengkapi `.github/workflows/build.yml` yang otomatis build APK di server GitHub.
Kamu cuma butuh akun GitHub, tidak perlu install apa-apa di komputer:

1. Buat repo baru di https://github.com/new (boleh **Public**, biar Actions gratis tanpa batas).
2. Di halaman repo kosong, klik **"uploading an existing file"**, lalu drag & drop **seluruh isi**
   folder `PointCounterNative/` (bukan file zip-nya, tapi isinya setelah di-extract) ke situ.
   Browser modern (Chrome/Edge) bisa drag folder lengkap beserta subfolder-nya sekaligus.
3. Commit langsung ke branch `main`.
4. Buka tab **Actions** di repo tersebut → workflow "Build APK" akan otomatis jalan (atau klik
   **Run workflow** kalau belum jalan otomatis).
5. Tunggu ±3-5 menit sampai statusnya centang hijau ✅.
6. Klik run yang selesai itu → scroll ke bagian **Artifacts** → download **PointCounter-debug-apk**
   (isinya file `.apk` siap install ke HP Android — aktifkan "Install dari sumber tidak dikenal"
   dulu di HP kamu).

## Cara build (kalau punya Android Studio)

## Struktur proyek

```
app/src/main/java/com/pointcounter/tezzyruok/
  data/         model + persistence (StorageRepository, Models.kt)
  viewmodel/    TournamentViewModel — seluruh business logic
  ui/screens/   6 layar: Setup, Match, Leaderboard, MostKill, Poster, Certificate
  ui/theme/     warna & tipografi (disamakan dengan CSS asli)
  util/         ImageExporter (poster/sertifikat), ExcelExporter (xlsx)
  MainActivity.kt  navigasi bottom-tab
```
