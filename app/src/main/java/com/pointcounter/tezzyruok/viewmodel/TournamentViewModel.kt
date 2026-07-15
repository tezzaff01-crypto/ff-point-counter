package com.pointcounter.tezzyruok.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pointcounter.tezzyruok.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Padanan native dari `let state = {...}` + semua fungsi manipulasinya di index.html
 * (updateConfig, updateTeamCount, saveMatch, calcRow, getLeaderboardRows, dst).
 */
class TournamentViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StorageRepository(app)

    private val _state = MutableStateFlow(TournamentState())
    val state: StateFlow<TournamentState> = _state.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        val loaded = repo.load()
        _state.value = if (loaded != null) ensureSessionTeams(loaded) else ensureSessionTeams(TournamentState())
    }

    fun consumeToast() { _toast.value = null }
    private fun toast(msg: String) { _toast.value = msg }

    /** Padanan initSessionTeams(): pastikan setiap sesi punya daftar tim, isi default "Tim N". */
    private fun ensureSessionTeams(s: TournamentState): TournamentState {
        val map = s.sessionTeams.toMutableMap()
        for (session in 1..s.sessions) {
            if (map[session] == null) {
                map[session] = (1..s.teamCount).map { Team("Tim $it") }
            }
        }
        return s.copy(sessionTeams = map)
    }

    private fun update(block: (TournamentState) -> TournamentState) {
        _state.value = block(_state.value)
        repo.save(_state.value)
    }

    // ================= SETUP / KONFIGURASI =================

    fun updateTourneyMeta(name: String, committee: String, tahapan: Tahapan) = update {
        val isGrandFinal = tahapan == Tahapan.GRAND_FINAL
        var s = it.copy(
            tourneyName = name,
            committeeName = committee,
            tahapan = tahapan,
            sessions = if (isGrandFinal) 1 else it.sessions
        )
        ensureSessionTeams(s)
    }

    fun updateSessionsAndMatches(sessions: Int, matchesPerSession: Int) = update {
        val s = it.copy(sessions = sessions.coerceIn(1, 8), matchesPerSession = matchesPerSession.coerceIn(1, 10))
        ensureSessionTeams(s)
    }

    /** Padanan updateTeamCount(): tambah/kurangi tim di semua sesi, bersihkan skor tim yang hilang. */
    fun updateTeamCount(n: Int) = update { st ->
        val count = n.coerceIn(2, 40)
        val newSessionTeams = st.sessionTeams.toMutableMap()
        for (session in 1..st.sessions) {
            val current = (newSessionTeams[session] ?: emptyList()).toMutableList()
            while (current.size < count) current.add(Team("Tim ${current.size + 1}"))
            newSessionTeams[session] = current.take(count)
        }
        val newScores = st.scores.mapValues { (_, matches) ->
            matches.mapValues { (_, teams) -> teams.filterKeys { idx -> idx < count } }
        }
        st.copy(teamCount = count, sessionTeams = newSessionTeams, scores = newScores)
    }

    fun renameTeam(session: Int, index: Int, name: String, rep: String) = update { st ->
        val teams = (st.sessionTeams[session] ?: emptyList()).toMutableList()
        if (index in teams.indices) teams[index] = teams[index].copy(name = name, rep = rep)
        st.copy(sessionTeams = st.sessionTeams + (session to teams))
    }

    fun updateRankPts(index: Int, value: Int) = update { st ->
        val list = st.rankPts.toMutableList()
        if (index in list.indices) list[index] = value.coerceAtLeast(0)
        st.copy(rankPts = list)
    }

    fun addRankSlot() = update { st -> st.copy(rankPts = st.rankPts + 0) }
    fun removeRankSlot() = update { st ->
        if (st.rankPts.size <= 1) st else st.copy(rankPts = st.rankPts.dropLast(1))
    }
    fun resetPointSettings() = update { st -> st.copy(rankPts = DEFAULT_RANK_PTS, killMultiplier = 1) }
    fun updateKillMultiplier(value: Int) = update { st -> st.copy(killMultiplier = value.coerceAtLeast(0)) }
    fun updateLogo(uri: String?) = update { st -> st.copy(logoUri = uri) }
    fun updateTop3Logo(rank: Int, uri: String?) = update { st ->
        val map = st.top3Logos.toMutableMap()
        if (uri == null) map.remove(rank) else map[rank] = uri
        st.copy(top3Logos = map)
    }

    /** Padanan copyFromPrevSession(). */
    fun copyFromPrevSession(session: Int) = update { st ->
        if (session <= 1) return@update st
        val prev = st.sessionTeams[session - 1] ?: return@update st
        st.copy(sessionTeams = st.sessionTeams + (session to prev.map { it.copy() }))
    }

    // ================= MATCH / SKOR =================

    /** Padanan saveMatch(): simpan kills+rank tiap tim untuk 1 match, hitung total otomatis. */
    fun saveMatchScores(session: Int, match: Int, entries: Map<Int, Pair<Int, Int?>>) = update { st ->
        val teamScores = entries.mapValues { (_, pair) ->
            val (kills, rank) = pair
            val rankPts = rank?.let { st.getRankPts(it) } ?: 0
            MatchScore(kills = kills, rank = rank, rankPts = rankPts, total = st.calcTotal(kills, rankPts))
        }
        val sessionScores = (st.scores[session] ?: emptyMap()).toMutableMap()
        sessionScores[match] = teamScores
        val newScores = st.scores.toMutableMap()
        newScores[session] = sessionScores
        toast("Match $match ${st.sessionLabel(session)} tersimpan!")
        st.copy(scores = newScores)
    }

    fun clearMatch(session: Int, match: Int) = update { st ->
        val sessionScores = (st.scores[session] ?: emptyMap()).toMutableMap()
        sessionScores.remove(match)
        val newScores = st.scores.toMutableMap()
        newScores[session] = sessionScores
        toast("Match direset!")
        st.copy(scores = newScores)
    }

    fun scoresFor(session: Int, match: Int): Map<Int, MatchScore> =
        _state.value.scores[session]?.get(match) ?: emptyMap()

    fun ranksTakenIn(session: Int, match: Int, excludingTeam: Int): Set<Int> =
        scoresFor(session, match).filterKeys { it != excludingTeam }.values.mapNotNull { it.rank }.toSet()

    fun countFilledMatches(): Int {
        val st = _state.value
        var filled = 0
        for (s in 1..st.sessions) for (m in 1..st.matchesPerSession) {
            if (!st.scores[s]?.get(m).isNullOrEmpty()) filled++
        }
        return filled
    }

    // ================= LEADERBOARD =================

    /** Padanan getLeaderboardRows(): total per tim di seluruh match sesi terpilih, dengan tie-breaker. */
    fun getLeaderboardRows(session: Int, sortBy: LeaderboardSort): List<LeaderboardRow> {
        val st = _state.value
        val teams = st.teamsFor(session)
        val sessionData = st.scores[session] ?: emptyMap()

        val rows = teams.mapIndexed { i, t ->
            var kills = 0; var rankPts = 0; var total = 0; var matches = 0
            for (m in 1..st.matchesPerSession) {
                val sc = sessionData[m]?.get(i) ?: continue
                kills += sc.kills; rankPts += sc.rankPts; total += sc.total; matches++
            }
            LeaderboardRow(i, t.name, t.rep, kills, rankPts, total, matches)
        }

        return when (sortBy) {
            LeaderboardSort.KILLS -> rows.sortedWith(compareByDescending<LeaderboardRow> { it.kills }
                .thenByDescending { it.total }.thenBy { it.teamIndex })
            LeaderboardSort.RANK_PTS -> rows.sortedWith(compareByDescending<LeaderboardRow> { it.rankPts }
                .thenByDescending { it.total }.thenBy { it.teamIndex })
            LeaderboardSort.TOTAL -> rows.sortedWith(compareByDescending<LeaderboardRow> { it.total }
                .thenByDescending { it.kills }.thenBy { it.teamIndex })
        }
    }

    // ================= MOST KILL =================

    fun setMostKill(session: Int, teamIndex: Int, entries: List<MostKillEntry>) = update { st ->
        val sessionMap = (st.mostKill[session] ?: emptyMap()).toMutableMap()
        sessionMap[teamIndex] = entries.take(3)
        val newMostKill = st.mostKill.toMutableMap()
        newMostKill[session] = sessionMap
        st.copy(mostKill = newMostKill)
    }

    // ================= RESET =================

    /** Padanan confirmResetAll(). */
    fun resetAll() {
        repo.clear()
        _state.value = ensureSessionTeams(TournamentState())
        toast("Semua data direset!")
    }

    fun resetScoresOnly() = update { st ->
        toast("Semua skor direset!")
        st.copy(scores = emptyMap(), mostKill = emptyMap())
    }
}
