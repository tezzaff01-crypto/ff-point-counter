package com.pointcounter.tezzyruok.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.pointcounter.tezzyruok.data.LeaderboardRow
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Penulis .xlsx minimal (format OOXML mentah, di-zip manual) — padanan native dari exceljs
 * yang dipakai versi WebView untuk export leaderboard ke Excel. Tanpa dependency eksternal.
 */
object ExcelExporter {

    fun exportLeaderboard(context: Context, sessionLabel: String, rows: List<LeaderboardRow>): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "leaderboard_${System.currentTimeMillis()}.xlsx")
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/PointCounter")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writeXlsx(out, sessionLabel, rows)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeXlsx(out: OutputStream, sessionLabel: String, rows: List<LeaderboardRow>) {
        val zip = ZipOutputStream(out)

        fun entry(name: String, content: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        entry(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
        )
        entry(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        )
        entry(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
        )
        entry(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="${escapeXml(sessionLabel.take(28))}" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""
        )

        val header = listOf("#", "Tim", "Perwakilan", "Kill", "Rank Pts", "Total", "Match")
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        sb.append(rowXml(1, header))
        rows.forEachIndexed { idx, r ->
            sb.append(
                rowXml(
                    idx + 2,
                    listOf((idx + 1).toString(), r.name, r.rep, r.kills.toString(), r.rankPts.toString(), r.total.toString(), r.matches.toString())
                )
            )
        }
        sb.append("</sheetData></worksheet>")
        entry("xl/worksheets/sheet1.xml", sb.toString())

        zip.close()
    }

    private fun rowXml(rowIndex: Int, cells: List<String>): String {
        val sb = StringBuilder("<row r=\"$rowIndex\">")
        cells.forEachIndexed { i, v ->
            val col = ('A' + i)
            sb.append("<c r=\"$col$rowIndex\" t=\"inlineStr\"><is><t>${escapeXml(v)}</t></is></c>")
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
