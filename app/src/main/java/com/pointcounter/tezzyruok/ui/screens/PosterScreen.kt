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

/** Padanan #page-poster: generate poster Top 3 dari leaderboard sesi terpilih, murni Canvas native. */
@Composable
fun PosterScreen(vm: TournamentViewModel, state: TournamentState) {
    val context = LocalContext.current
    var session by remember { mutableIntStateOf(1) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FFCard(title = "Generate Poster Top 3") {
            Text(
                "Poster dibuat otomatis dari data leaderboard — digambar langsung dengan Canvas native Android, tanpa WebView.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in 1..state.sessions) {
                    FilterChip(selected = session == s, onClick = { session = s }, label = { Text(state.sessionLabel(s)) })
                }
            }
            Button(onClick = {
                val top3 = vm.getLeaderboardRows(session, LeaderboardSort.TOTAL).take(3)
                bitmap = ImageExporter.generateTop3Poster(state, state.tourneyName, top3)
            }, modifier = Modifier.fillMaxWidth()) { Text("Generate Poster") }
        }

        bitmap?.let { bmp ->
            Image(bmp.asImageBitmap(), contentDescription = "Poster Top 3", modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val ok = ImageExporter.saveToGallery(context, bmp, "poster_top3_${System.currentTimeMillis()}")
                    Toast.makeText(context, if (ok) "Poster tersimpan di galeri" else "Gagal menyimpan", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Download Poster (PNG)") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
