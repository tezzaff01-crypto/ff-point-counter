package com.pointcounter.tezzyruok.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pointcounter.tezzyruok.data.LeaderboardSort
import com.pointcounter.tezzyruok.data.TournamentState
import com.pointcounter.tezzyruok.ui.components.FFCard
import com.pointcounter.tezzyruok.util.ImageExporter
import com.pointcounter.tezzyruok.viewmodel.TournamentViewModel

/** Padanan #page-certificate: generate e-sertifikat Juara 1-3 otomatis dari leaderboard, murni Canvas native. */
@Composable
fun CertificateScreen(vm: TournamentViewModel, state: TournamentState) {
    val context = LocalContext.current
    var session by remember { mutableIntStateOf(1) }
    var certs by remember { mutableStateOf<List<Pair<Int, Bitmap>>>(emptyList()) }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FFCard(title = "Generate Sertifikat Juara 1-3") {
            Text(
                "Sertifikat dibuat otomatis dari data leaderboard — nama tim Juara 1, 2, 3 ditempel ke template memakai Canvas native.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in 1..state.sessions) {
                    FilterChip(selected = session == s, onClick = { session = s }, label = { Text(state.sessionLabel(s)) })
                }
            }
            Button(onClick = {
                val top3 = vm.getLeaderboardRows(session, LeaderboardSort.TOTAL).take(3)
                certs = top3.mapIndexed { idx, row ->
                    (idx + 1) to ImageExporter.generateCertificate(state.tourneyName, state.committeeName, row.name, idx + 1)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Generate Sertifikat") }
        }

        certs.forEach { (rank, bmp) ->
            FFCard(title = "Juara $rank") {
                Image(bmp.asImageBitmap(), contentDescription = "Sertifikat Juara $rank", modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        val ok = ImageExporter.saveToGallery(context, bmp, "sertifikat_juara${rank}_${System.currentTimeMillis()}")
                        Toast.makeText(context, if (ok) "Sertifikat tersimpan di galeri" else "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Download Sertifikat Juara $rank") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
