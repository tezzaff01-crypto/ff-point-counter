package com.pointcounter.tezzyruok.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pointcounter.tezzyruok.data.TournamentState
import com.pointcounter.tezzyruok.ui.components.FFCard
import com.pointcounter.tezzyruok.ui.theme.FFGreen
import com.pointcounter.tezzyruok.ui.theme.FFOrange
import com.pointcounter.tezzyruok.viewmodel.TournamentViewModel

/** Padanan #page-match: input kill & rank per tim untuk 1 match, dengan validasi rank unik. */
@Composable
fun MatchScreen(vm: TournamentViewModel, state: TournamentState) {
    var session by remember { mutableIntStateOf(1) }
    var match by remember { mutableIntStateOf(1) }
    val teams = state.teamsFor(session.coerceAtMost(state.sessions).coerceAtLeast(1))

    // Working copy of kills+rank keyed by team index, seeded from saved scores.
    var working by remember(session, match, state.matchesPerSession) {
        val saved = vm.scoresFor(session, match)
        mutableStateOf(teams.indices.associateWith { i -> (saved[i]?.kills ?: 0) to saved[i]?.rank })
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            FFCard(title = "${state.sessionLabel(session)} — Match $match") {
                if (state.sessions > 1) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (s in 1..state.sessions) {
                            FilterChip(selected = session == s, onClick = { session = s; match = 1 }, label = { Text(state.sessionLabel(s)) })
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in 1..state.matchesPerSession) {
                        val filled = !vm.scoresFor(session, m).isEmpty()
                        FilterChip(
                            selected = match == m, onClick = { match = m },
                            label = { Text("Match $m${if (filled) " ✓" else ""}") }
                        )
                    }
                }
            }
        }

        items(teams.size) { i ->
            val team = teams[i]
            val (kills, rank) = working[i] ?: (0 to null)
            val takenRanks = working.filterKeys { it != i }.values.mapNotNull { it.second }.toSet()
            val rankPts = rank?.let { state.getRankPts(it) } ?: 0
            val total = state.calcTotal(kills, rankPts)

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${team.name}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Total: $total", color = FFOrange, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (kills == 0) "" else kills.toString(),
                            onValueChange = { v ->
                                val k = v.toIntOrNull() ?: 0
                                working = working + (i to (k to rank))
                            },
                            label = { Text("Kills") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        RankDropdown(
                            teamCount = teams.size, selected = rank, taken = takenRanks,
                            onSelect = { r -> working = working + (i to (kills to r)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    vm.saveMatchScores(session, match, working.mapValues { it.value })
                },
                colors = ButtonDefaults.buttonColors(containerColor = FFGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan Match $match") }
        }
        item {
            OutlinedButton(
                onClick = { vm.clearMatch(session, match); working = teams.indices.associateWith { 0 to (null as Int?) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reset Match Ini") }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun RankDropdown(teamCount: Int, selected: Int?, taken: Set<Int>, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (selected != null) "Rank #$selected" else "Pilih Rank")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("— (belum)") }, onClick = { onSelect(null); expanded = false })
            for (r in 1..teamCount) {
                DropdownMenuItem(
                    text = { Text("Rank #$r") },
                    enabled = r !in taken || r == selected,
                    onClick = { onSelect(r); expanded = false }
                )
            }
        }
    }
}
