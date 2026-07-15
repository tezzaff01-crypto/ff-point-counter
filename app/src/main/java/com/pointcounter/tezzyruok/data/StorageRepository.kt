package com.pointcounter.tezzyruok.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistensi lokal berbasis file JSON di storage internal aplikasi.
 * Ini padanan native dari localStorage.setItem/getItem('ff_tournament_state_v1', ...)
 * pada versi WebView — tidak butuh internet maupun WebView sama sekali.
 */
class StorageRepository(private val context: Context) {

    private val file: File get() = File(context.filesDir, "ff_tournament_state_v1.json")

    fun save(state: TournamentState) {
        val obj = JSONObject()
        obj.put("sessions", state.sessions)
        obj.put("matchesPerSession", state.matchesPerSession)
        obj.put("teamCount", state.teamCount)
        obj.put("tourneyName", state.tourneyName)
        obj.put("committeeName", state.committeeName)
        obj.put("tahapan", state.tahapan.label)
        obj.put("logoUri", state.logoUri)
        obj.put("killMultiplier", state.killMultiplier)
        obj.put("rankPts", JSONArray(state.rankPts))

        val sessionTeamsJson = JSONObject()
        state.sessionTeams.forEach { (s, teams) ->
            val arr = JSONArray()
            teams.forEach { t ->
                arr.put(JSONObject().put("name", t.name).put("rep", t.rep))
            }
            sessionTeamsJson.put(s.toString(), arr)
        }
        obj.put("sessionTeams", sessionTeamsJson)

        val scoresJson = JSONObject()
        state.scores.forEach { (s, matches) ->
            val matchesJson = JSONObject()
            matches.forEach { (m, teamScores) ->
                val teamsJson = JSONObject()
                teamScores.forEach { (idx, sc) ->
                    teamsJson.put(
                        idx.toString(),
                        JSONObject()
                            .put("kills", sc.kills)
                            .put("rank", sc.rank ?: JSONObject.NULL)
                            .put("rankPts", sc.rankPts)
                            .put("total", sc.total)
                    )
                }
                matchesJson.put(m.toString(), teamsJson)
            }
            scoresJson.put(s.toString(), matchesJson)
        }
        obj.put("scores", scoresJson)

        val top3LogosJson = JSONObject()
        state.top3Logos.forEach { (rank, uri) -> top3LogosJson.put(rank.toString(), uri) }
        obj.put("top3Logos", top3LogosJson)

        val mostKillJson = JSONObject()
        state.mostKill.forEach { (s, teams) ->
            val teamsJson = JSONObject()
            teams.forEach { (idx, entries) ->
                val arr = JSONArray()
                entries.forEach { e -> arr.put(JSONObject().put("name", e.name).put("kills", e.kills)) }
                teamsJson.put(idx.toString(), arr)
            }
            mostKillJson.put(s.toString(), teamsJson)
        }
        obj.put("mostKill", mostKillJson)

        obj.put("_savedAt", System.currentTimeMillis())
        obj.put("_version", "native_v1")

        file.writeText(obj.toString())
    }

    fun load(): TournamentState? {
        if (!file.exists()) return null
        return try {
            val obj = JSONObject(file.readText())

            val sessionTeams = mutableMapOf<Int, List<Team>>()
            obj.optJSONObject("sessionTeams")?.let { stj ->
                stj.keys().forEach { key ->
                    val arr = stj.getJSONArray(key)
                    val list = (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        Team(o.optString("name"), o.optString("rep"))
                    }
                    sessionTeams[key.toInt()] = list
                }
            }

            val scores = mutableMapOf<Int, Map<Int, Map<Int, MatchScore>>>()
            obj.optJSONObject("scores")?.let { sj ->
                sj.keys().forEach { sKey ->
                    val matchesJson = sj.getJSONObject(sKey)
                    val matches = mutableMapOf<Int, Map<Int, MatchScore>>()
                    matchesJson.keys().forEach { mKey ->
                        val teamsJson = matchesJson.getJSONObject(mKey)
                        val teams = mutableMapOf<Int, MatchScore>()
                        teamsJson.keys().forEach { tKey ->
                            val o = teamsJson.getJSONObject(tKey)
                            teams[tKey.toInt()] = MatchScore(
                                kills = o.optInt("kills", 0),
                                rank = if (o.isNull("rank")) null else o.optInt("rank"),
                                rankPts = o.optInt("rankPts", 0),
                                total = o.optInt("total", 0)
                            )
                        }
                        matches[mKey.toInt()] = teams
                    }
                    scores[sKey.toInt()] = matches
                }
            }

            val top3Logos = mutableMapOf<Int, String>()
            obj.optJSONObject("top3Logos")?.let { j ->
                j.keys().forEach { k -> top3Logos[k.toInt()] = j.getString(k) }
            }

            val mostKill = mutableMapOf<Int, Map<Int, List<MostKillEntry>>>()
            obj.optJSONObject("mostKill")?.let { mkj ->
                mkj.keys().forEach { sKey ->
                    val teamsJson = mkj.getJSONObject(sKey)
                    val teams = mutableMapOf<Int, List<MostKillEntry>>()
                    teamsJson.keys().forEach { tKey ->
                        val arr = teamsJson.getJSONArray(tKey)
                        teams[tKey.toInt()] = (0 until arr.length()).map { i ->
                            val o = arr.getJSONObject(i)
                            MostKillEntry(o.optString("name"), o.optInt("kills"))
                        }
                    }
                    mostKill[sKey.toInt()] = teams
                }
            }

            val rankPtsArr = obj.optJSONArray("rankPts")
            val rankPts = if (rankPtsArr != null) {
                (0 until rankPtsArr.length()).map { rankPtsArr.optInt(it, 0) }
            } else DEFAULT_RANK_PTS

            TournamentState(
                sessions = obj.optInt("sessions", 1),
                matchesPerSession = obj.optInt("matchesPerSession", 3),
                teamCount = obj.optInt("teamCount", 12),
                sessionTeams = sessionTeams,
                scores = scores,
                tourneyName = obj.optString("tourneyName", ""),
                committeeName = obj.optString("committeeName", ""),
                tahapan = Tahapan.entries.find { it.label == obj.optString("tahapan") } ?: Tahapan.KUALIFIKASI,
                logoUri = obj.optString("logoUri", null).takeUnless { it == "null" },
                top3Logos = top3Logos,
                rankPts = rankPts,
                killMultiplier = obj.optInt("killMultiplier", 1),
                mostKill = mostKill
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
