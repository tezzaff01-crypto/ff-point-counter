package com.pointcounter.tezzyruok.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pointcounter.tezzyruok.data.LeaderboardSort
import com.pointcounter.tezzyruok.data.TournamentState
import com.pointcounter.tezzyruok.ui.theme.*
import com.pointcounter.tezzyruok.util.ExcelExporter
import com.pointcounter.tezzyruok.viewmodel.TournamentViewModel

/** Padanan #page-leaderboard: klasemen per sesi, bisa disort total/kill/rankpts. */
@Composable
fun LeaderboardScreen(vm: TournamentViewModel, state: TournamentState) {
    var session by remember { mutableIntStateOf(1) }
    var sortBy by remember { mutableStateOf(LeaderboardSort.TOTAL) }
    val rows = vm.getLeaderboardRows(session.coerceIn(1, state.sessions), sortBy)
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            for (s in 1..state.sessions) {
                FilterChip(selected = session == s, onClick = { session = s }, label = { Text(state.sessionLabel(s)) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(selected = sortBy == LeaderboardSort.TOTAL, onClick = { sortBy = LeaderboardSort.TOTAL }, label = { Text("Total") })
            FilterChip(selected = sortBy == LeaderboardSort.KILLS, onClick = { sortBy = LeaderboardSort.KILLS }, label = { Text("Kill") })
            FilterChip(selected = sortBy == LeaderboardSort.RANK_PTS, onClick = { sortBy = LeaderboardSort.RANK_PTS }, label = { Text("Rank Pts") })
        }
        OutlinedButton(
            onClick = {
                val ok = ExcelExporter.exportLeaderboard(context, state.sessionLabel(session), rows)
                Toast.makeText(context, if (ok) "Leaderboard diekspor ke Download/PointCounter" else "Gagal ekspor", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("Export ke Excel (.xlsx)") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows.size) { pos ->
                val r = rows[pos]
                val medal = when (pos) { 0 -> FFGold; 1 -> FFSilver; 2 -> FFBronze; else -> null }
                Card {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "${pos + 1}", fontWeight = FontWeight.Black,
                            color = medal ?: FFMuted, modifier = Modifier.width(28.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.Bold)
                            if (r.rep.isNotBlank()) Text(r.rep, style = MaterialTheme.typography.bodySmall, color = FFMuted)
                            Text("${r.matches} match dimainkan", style = MaterialTheme.typography.bodySmall, color = FFMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${r.total} pts", fontWeight = FontWeight.Bold, color = FFOrange)
                            Text("Kill ${r.kills} · Rank ${r.rankPts}", style = MaterialTheme.typography.bodySmall, color = FFMuted)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
