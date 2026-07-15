package com.pointcounter.tezzyruok.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.MediaStore
import com.pointcounter.tezzyruok.data.LeaderboardRow
import com.pointcounter.tezzyruok.data.TournamentState

/**
 * Generator poster & sertifikat native memakai android.graphics.Canvas.
 * Ini padanan langsung dari fitur generatePoster()/generateCertificates() yang di versi
 * WebView memakai html2canvas — di sini digambar langsung ke Bitmap, tanpa WebView/JS sama sekali.
 */
object ImageExporter {

    /** Poster Top 3 — dipakai untuk tab Poster. */
    fun generateTop3Poster(state: TournamentState, tourneyName: String, top3: List<LeaderboardRow>): Bitmap {
        val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Background gradient mengikuti --ff-dark & aksen ungu/biru dari CSS asli
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.parseColor("#05060A"), Color.parseColor("#12153A"), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 64f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        c.drawText(tourneyName.ifBlank { "TOURNAMENT" }.uppercase(), w / 2f, 140f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A92A8"); textSize = 32f; textAlign = Paint.Align.CENTER
        }
        c.drawText("TOP 3 LEADERBOARD", w / 2f, 190f, subPaint)

        val medalColors = listOf("#FFD700", "#C0C0C0", "#CD7F32")
        val startY = 320f
        top3.take(3).forEachIndexed { idx, row ->
            val y = startY + idx * 260f
            val boxPaint = Paint().apply { color = Color.parseColor("#171B28") }
            c.drawRoundRect(RectF(60f, y, w - 60f, y + 220f), 24f, 24f, boxPaint)

            val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(medalColors[idx]); textSize = 90f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
            }
            c.drawText("#${idx + 1}", 90f, y + 140f, rankPaint)

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 48f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
            }
            c.drawText(row.name, 260f, y + 90f, namePaint)

            val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8A92A8"); textSize = 30f; textAlign = Paint.Align.LEFT
            }
            c.drawText("Kill ${row.kills} · Rank Pts ${row.rankPts}", 260f, y + 135f, statPaint)

            val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6C87FF"); textSize = 56f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
            }
            c.drawText("${row.total}", w - 90f, y + 100f, totalPaint)
            val ptsLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8A92A8"); textSize = 24f; textAlign = Paint.Align.RIGHT
            }
            c.drawText("PTS", w - 90f, y + 130f, ptsLabel)
        }
        return bmp
    }

    /** Sertifikat Juara 1/2/3 — dipakai untuk tab Sertifikat. */
    fun generateCertificate(tourneyName: String, committeeName: String, teamName: String, rank: Int): Bitmap {
        val w = 1600; val h = 1131 // rasio landscape A4-ish
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#06070C"))

        val borderColor = when (rank) { 1 -> "#FFD700"; 2 -> "#C0C0C0"; else -> "#CD7F32" }
        val borderPaint = Paint().apply { color = Color.parseColor(borderColor); style = Paint.Style.STROKE; strokeWidth = 14f }
        c.drawRect(40f, 40f, w - 40f, h - 40f, borderPaint)
        val innerPaint = Paint().apply { color = Color.parseColor("#262B3B"); style = Paint.Style.STROKE; strokeWidth = 2f }
        c.drawRect(60f, 60f, w - 60f, h - 60f, innerPaint)

        fun centered(text: String, y: Float, size: Float, color: String, bold: Boolean = false) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.parseColor(color); textSize = size; isFakeBoldText = bold; textAlign = Paint.Align.CENTER
            }
            c.drawText(text, w / 2f, y, p)
        }

        centered("SERTIFIKAT PENGHARGAAN", 220f, 42f, "#8A92A8")
        centered(tourneyName.ifBlank { "TOURNAMENT" }.uppercase(), 300f, 56f, "#FFFFFF", bold = true)
        centered("Dengan bangga mempersembahkan sertifikat ini kepada", 420f, 30f, "#8A92A8")
        centered(teamName, 540f, 90f, borderColor, bold = true)
        val rankLabel = when (rank) { 1 -> "JUARA 1"; 2 -> "JUARA 2"; else -> "JUARA 3" }
        centered(rankLabel, 640f, 50f, borderColor, bold = true)
        centered("atas prestasi dan dedikasi dalam mengikuti turnamen ini", 740f, 28f, "#8A92A8")
        if (committeeName.isNotBlank()) centered(committeeName, h - 120f, 26f, "#F3F5FA")
        centered("Panitia Penyelenggara", h - 90f, 22f, "#8A92A8")

        return bmp
    }

    /** Simpan Bitmap ke galeri (MediaStore) sebagai PNG — padanan tombol Download versi WebView. */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PointCounter")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
