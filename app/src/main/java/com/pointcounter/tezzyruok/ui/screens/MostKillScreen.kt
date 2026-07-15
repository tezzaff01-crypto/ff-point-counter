package com.pointcounter.tezzyruok.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pointcounter.tezzyruok.data.MostKillEntry
import com.pointcounter.tezzyruok.data.TournamentState
import com.pointcounter.tezzyruok.ui.components.FFCard
import com.pointcounter.tezzyruok.viewmodel.TournamentViewModel

/** Padanan #page-mostkill: catat sampai 3 top-killer per tim per sesi (nama pemain + jumlah kill). */
@Composable
fun MostKillScreen(vm: TournamentViewModel, state: TournamentState) {
    var session by remember { mutableIntStateOf(1) }
    val teams = state.teamsFor(session.coerceIn(1, state.sessions))

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            for (s in 1..state.sessions) {
                FilterChip(selected = session == s, onClick = { session = s }, label = { Text(state.sessionLabel(s)) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(teams.size) { i ->
                val saved = state.mostKill[session]?.get(i) ?: emptyList()
                var entries by remember(session, i, saved) {
                    mutableStateOf((0..2).map { saved.getOrNull(it) ?: MostKillEntry() })
                }
                FFCard(title = "${i + 1}. ${teams[i].name}") {
                    entries.forEachIndexed { slot, entry ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = entry.name,
                                onValueChange = { v ->
                                    entries = entries.toMutableList().also { it[slot] = entry.copy(name = v) }
                                    vm.setMostKill(session, i, entries.filter { it.name.isNotBlank() })
                                },
                                label = { Text("Nama Pemain #${slot + 1}") }, modifier = Modifier.weight(2f), singleLine = true
                            )
                            OutlinedTextField(
                                value = if (entry.kills == 0) "" else entry.kills.toString(),
                                onValueChange = { v ->
                                    val k = v.toIntOrNull() ?: 0
                                    entries = entries.toMutableList().also { it[slot] = entry.copy(kills = k) }
                                    vm.setMostKill(session, i, entries.filter { it.name.isNotBlank() })
                                },
                                label = { Text("Kill") }, modifier = Modifier.weight(1f), singleLine = true
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
