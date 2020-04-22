package ma.covid.shield

import android.content.Context
import android.util.Log

import ma.covid.shield.db.DataStorage
import ma.covid.shield.network.NetworkManager

import org.json.JSONException
import org.json.JSONObject

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.ArrayList

import java.lang.Math.max
import java.lang.Math.min

class TagsManager(context: Context, internal var listener: TagsManagerListener) {
    internal var net: NetworkManager

    init {
        net = NetworkManager.getInstance(context)
        net.registerListener(object : NetworkManager.DataListener {
            override fun onReceived(json: JSONObject) {
                listener.onMatchCandidate(json)
            }
        })
    }

    fun match(storageList: List<DataStorage>, json: JSONObject): Float {
        var score = 0f
        val all_matches = ArrayList<TagMatch>()
        for (storage in storageList) {
            val prefix = storage.prefix
            try {
                val array = json.getJSONArray(prefix)
                for (i in 0 until array.length()) {
                    val el = array.getJSONObject(i)
                    val group1 = DataStorage.readElementsFromJson(el)
                    val instant = group1.get(0).instant
                    val group2 = storage.readElements(instant)
                    val matches = matchTags(group1, group2)
                    all_matches.addAll(matches)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        score = MatchScorer.computeScore(all_matches)
        Log.v(TAG, "Computed score: $score")
        return score
    }

    private fun matchTags(
        l1: List<DataStorage.TagElementGroup>,
        l2: List<DataStorage.TagElementGroup>
    ): List<TagMatch> {
        val result = ArrayList<TagMatch>()

        for (i in l1.indices) {
            val element = l1[i]

            if (element.tags.size == 0)
                continue

            val candidateIndex = findMatchCandidate(element, l2)
            if (candidateIndex < 0)
                continue

            Log.v(TAG, "Index of match candidate: $candidateIndex")

            val candidate = l2[candidateIndex]

            val tm = TagMatch()
            var match = false

            val l = min(element.tags.size, candidate.tags.size)

            val start = max(element.instant.getEpochSecond(), element.instant.getEpochSecond())
            val end = min(
                if (i < l1.size - 1) l1[i + 1].instant.getEpochSecond() else element.instant.truncatedTo(
                    ChronoUnit.HOURS
                ).getEpochSecond() + 3600,
                if (candidateIndex < l2.size - 1) l2[candidateIndex + 1].instant.getEpochSecond() else candidate.instant.truncatedTo(
                    ChronoUnit.HOURS
                ).getEpochSecond() + 3600
            )

            for (j in 0 until l) {
                val e1 = element.tags.get(j)
                val e2 = candidate.tags.get(j)
                if (e1.id == e2.id && e1.name == e2.name) {
                    tm.matches[j] = true
                    tm.duration = (end - start).toInt()
                    match = true
                    Log.v(TAG, "match found at: " + candidateIndex + " duration: " + tm.duration)
                }
            }
            if (match)
                result.add(tm)
        }
        return result

    }

    private fun findMatchCandidate(
        el: DataStorage.TagElementGroup?,
        list: List<DataStorage.TagElementGroup>
    ): Int {
        var result = -1
        if (!list.isEmpty() && el != null) {
            var afterIndex = -1
            for (i in list.indices) {
                val candidate = list[i]
                if (candidate.instant.isAfter(el!!.instant)) {
                    afterIndex = i
                    break
                }
            }

            if (afterIndex > 0)
                result = afterIndex - 1
            else {
                result = list.size - 1
            }
        }
        return result
    }

    public fun uploadTags(storageList: List<DataStorage>, start: Instant, end: Instant) {
        net.uploadTags(storageList, start, end, object : NetworkManager.NetworkCallback {
            override fun onResponse(response: JSONObject) {
                listener.onUpload(true)

            }

            override fun onError() {
                listener.onUpload(false)
            }
        })

    }

    class TagMatch {

        var associated: Boolean = false
        var matches = booleanArrayOf(false, false, false)
        var duration: Int = 0
    }

    interface TagsManagerListener {
        fun onMatchCandidate(json: JSONObject)

        fun onUpload(success: Boolean)
    }

    companion object {

        val TAG = "TagsManager"

    }

}
