package com.pointcounter.tezzyruok.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pointcounter.tezzyruok.data.Tahapan
import com.pointcounter.tezzyruok.data.TournamentState
import com.pointcounter.tezzyruok.ui.components.FFCard
import com.pointcounter.tezzyruok.viewmodel.TournamentViewModel

/** Padanan #page-setup: identitas turnamen, jumlah sesi/match/tim, daftar tim, sistem poin. */
@Composable
fun SetupScreen(vm: TournamentViewModel, state: TournamentState) {
    var name by remember(state.tourneyName) { mutableStateOf(state.tourneyName) }
    var committee by remember(state.committeeName) { mutableStateOf(state.committeeName) }
    var selectedSession by remember { mutableIntStateOf(1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FFCard(title = "Identitas Turnamen") {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; vm.updateTourneyMeta(name, committee, state.tahapan) },
                    label = { Text("Nama Turnamen") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = committee, onValueChange = { committee = it; vm.updateTourneyMeta(name, committee, state.tahapan) },
                    label = { Text("Nama Panitia") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Tahapan", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tahapan.entries.forEach { t ->
                        FilterChip(
                            selected = state.tahapan == t,
                            onClick = { vm.updateTourneyMeta(name, committee, t) },
                            label = { Text(t.label, maxLines = 1) }
                        )
                    }
                }
            }
        }

        item {
            FFCard(title = "Struktur Turnamen") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Jumlah Sesi", value = state.sessions,
                        enabled = state.tahapan != Tahapan.GRAND_FINAL,
                        onChange = { vm.updateSessionsAndMatches(it, state.matchesPerSession) },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Match / Sesi", value = state.matchesPerSession,
                        onChange = { vm.updateSessionsAndMatches(state.sessions, it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                NumberField(
                    label = "Jumlah Tim", value = state.teamCount,
                    onChange = { vm.updateTeamCount(it) }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${state.sessions} sesi × ${state.matchesPerSession} match = ${state.sessions * state.matchesPerSession} match total",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            FFCard(title = "Daftar Tim") {
                if (state.sessions > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (s in 1..state.sessions) {
                            FilterChip(
                                selected = selectedSession == s, onClick = { selectedSession = s },
                                label = { Text(state.sessionLabel(s)) }
                            )
                        }
                    }
                    if (selectedSession > 1) {
                        TextButton(onClick = { vm.copyFromPrevSession(selectedSession) }) {
                            Text("Salin dari sesi sebelumnya")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                val teams = state.teamsFor(selectedSession.coerceAtMost(state.sessions).coerceAtLeast(1))
                teams.forEachIndexed { idx, team ->
                    var teamName by remember(team.name) { mutableStateOf(team.name) }
                    var teamRep by remember(team.rep) { mutableStateOf(team.rep) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${idx + 1}", modifier = Modifier.width(20.dp), style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = teamName, onValueChange = {
                                teamName = it
                                vm.renameTeam(selectedSession, idx, teamName, teamRep)
                            },
                            label = { Text("Nama Tim") }, modifier = Modifier.weight(1f), singleLine = true
                        )
                        OutlinedTextField(
                            value = teamRep, onValueChange = {
                                teamRep = it
                                vm.renameTeam(selectedSession, idx, teamName, teamRep)
                            },
                            label = { Text("Perwakilan") }, modifier = Modifier.weight(1f), singleLine = true
                        )
                    }
                }
            }
        }

        item {
            FFCard(title = "Sistem Poin") {
                Text("Poin Kill (kali lipat)", style = MaterialTheme.typography.labelSmall)
                NumberField(label = "", value = state.killMultiplier, onChange = { vm.updateKillMultiplier(it) })
                Spacer(Modifier.height(8.dp))
                Text("Poin per Rank (posisi #1 → #${state.rankPts.size})", style = MaterialTheme.typography.labelSmall)
                state.rankPts.forEachIndexed { idx, pt ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("#${idx + 1}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = pt.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { vm.updateRankPts(idx, it) } },
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.addRankSlot() }) { Text("+ Slot") }
                    OutlinedButton(onClick = { vm.removeRankSlot() }) { Icon(Icons.Filled.Delete, null); Text(" Slot") }
                    TextButton(onClick = { vm.resetPointSettings() }) { Text("Reset Default") }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun NumberField(label: String, value: Int, enabled: Boolean = true, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { t -> text = t; t.toIntOrNull()?.let(onChange) },
        label = { if (label.isNotEmpty()) Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = modifier
    )
}
