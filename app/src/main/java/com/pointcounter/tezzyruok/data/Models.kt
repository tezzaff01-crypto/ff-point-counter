package com.pointcounter.tezzyruok.data

/** Satu tim dalam sebuah sesi. Setara dengan { name, rep } di state.sessionTeams[s][i] JS. */
data class Team(
    val name: String,
    val rep: String = ""
)

/** Skor satu tim pada satu match. Setara dengan state.scores[session][match][teamIndex]. */
data class MatchScore(
    val kills: Int = 0,
    val rank: Int? = null,
    val rankPts: Int = 0,
    val total: Int = 0
)

/** Satu entri "most kill" (top killer per tim per sesi). */
data class MostKillEntry(
    val name: String = "",
    val kills: Int = 0
)

enum class Tahapan(val label: String) {
    KUALIFIKASI("KUALIFIKASI"),
    SEMIFINAL("SEMIFINAL"),
    GRAND_FINAL("GRAND FINAL")
}

val DEFAULT_RANK_PTS = listOf(12, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

/**
 * State turnamen secara keseluruhan — padanan native dari objek `state` pada index.html
 * (baris "STATE" di app WebView lama). Semua field & default nilai disamakan 1:1.
 */
data class TournamentState(
    val sessions: Int = 1,
    val matchesPerSession: Int = 3,
    val teamCount: Int = 12,
    val sessionTeams: Map<Int, List<Team>> = emptyMap(),
    // scores[session][match][teamIndex]
    val scores: Map<Int, Map<Int, Map<Int, MatchScore>>> = emptyMap(),
    val tourneyName: String = "",
    val committeeName: String = "",
    val tahapan: Tahapan = Tahapan.KUALIFIKASI,
    val logoUri: String? = null,
    // top3Logos[rank] = uri
    val top3Logos: Map<Int, String> = emptyMap(),
    val rankPts: List<Int> = DEFAULT_RANK_PTS,
    val killMultiplier: Int = 1,
    // mostKill[session][teamIndex] = list of up to 3 entries
    val mostKill: Map<Int, Map<Int, List<MostKillEntry>>> = emptyMap()
) {
    fun getRankPts(rank: Int): Int = rankPts.getOrElse(rank - 1) { 0 }
    fun calcKillPts(kills: Int): Int = kills * killMultiplier
    fun calcTotal(kills: Int, rankPts: Int): Int = kills * killMultiplier + rankPts
    fun sessionLabel(s: Int): String = if (tahapan == Tahapan.GRAND_FINAL) "Grand Final" else "Sesi $s"
    fun teamsFor(session: Int): List<Team> = sessionTeams[session] ?: emptyList()
}

/** Baris leaderboard terhitung — padanan dari objek yang dihasilkan getLeaderboardRows() di JS. */
data class LeaderboardRow(
    val teamIndex: Int,
    val name: String,
    val rep: String,
    val kills: Int,
    val rankPts: Int,
    val total: Int,
    val matches: Int
)

enum class LeaderboardSort { TOTAL, KILLS, RANK_PTS }
