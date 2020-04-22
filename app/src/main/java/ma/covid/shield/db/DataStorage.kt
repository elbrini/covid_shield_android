package ma.covid.shield.db

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.time.Instant
import java.time.ZoneOffset
import java.util.ArrayList

class DataStorage(private val context: Context, val prefix: String) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val sharedPreferences: SharedPreferences


    init {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {}


    fun saveElements(tagsGroup: TagElementGroup, optimized: Boolean): Boolean {

        var res = false


        val elements = tagsGroup.tags

        val json = getSavedJson(tagsGroup.instant)

        var array = JSONArray()

        if (json != null) {
            Log.v(TAG, "JSON exists")

            try {
                array = json.getJSONArray("data")
                if (optimized) {
                    Log.v(TAG, "Optimized")

                    val latest = array.getJSONObject(array.length() - 1).getJSONArray("tags")


                    Log.v(TAG, "Latest size: " + latest.length())


                    if (latest.length() == elements.size) {
                        var match = true
                        for (j in elements.indices) {
                            val c = latest.getJSONObject(j)
                            val e = elements[j]
                            if (e.id != c.get("id")) {
                                match = false
                                break
                            }
                        }

                        if (match) {
                            Log.v(TAG, "Elements match, ignore!")
                            return false
                        }
                    }

                }

            } catch (e: JSONException) {
                e.printStackTrace()

            }

        }

        try {

            val offset = array.length()

            val g = JSONObject()
            val t = JSONArray()
            for (i in elements.indices) {
                val el = elements[i]
                val o = JSONObject()
                o.put("id", el.id)
                o.put("name", el.name)
                o.put("indicator", el.indicator)
                t.put(i, o)
            }
            g.put("date", tagsGroup.instant)
            g.put("tags", t)
            array.put(offset, g)

            val result = JSONObject()
            result.put("data", array)
            Log.v(TAG, result.toString())
            val editor = sharedPreferences.edit()
            editor.putString(keyFromInstant(tagsGroup.instant), result.toString())
            editor.commit()

            res = true

        } catch (e: JSONException) {
            e.printStackTrace()
        }


        /*TagsManager mgr = new TagsManager();
            List<TagElementGroup> list = new ArrayList<>();
            TagElementGroup first = new TagElementGroup();
            first.instant = first.instant.truncatedTo(ChronoUnit.HOURS);
            list.add(first);
            list.add(tagsGroup);
            List<TagsManager.TagMatch> matches = mgr.matchTags(readElements(tagsGroup.instant), list);
            Log.v(TAG, "MATCHES: " + matches.size() + "SCORE: " +MatchScorer.computeScore(matches));*/

        return res
    }

    fun getSavedJson(instant: Instant): JSONObject? {

        var res: JSONObject? = null


        val strJson = sharedPreferences.getString(keyFromInstant(instant), "")

        if (strJson !== "") {
            Log.v(TAG, "JSON exists")
            try {
                res = JSONObject(strJson!!)
            } catch (e: JSONException) {
                e.printStackTrace()

            }

        } else
            Log.v(TAG, "JSON DO NOT exists")
        return res
    }

    private fun keyFromInstant(instant: Instant): String {
        val d = instant.atZone(ZoneOffset.UTC)

        val prefix = (this.prefix + "_"
                + d.year + "_"
                + d.month + "_"
                + d.dayOfMonth + "_"
                + d.hour)

        Log.v(TAG, "Prefix:$prefix")

        return prefix
    }

    fun readElements(instant: Instant): List<TagElementGroup> {
        return readElementsFromJson(getSavedJson(instant))
    }


    class TagElement : Comparable<TagElement> {
        var id: String? = null
        var name: String? = null
        var indicator: Int = 0

        override fun compareTo(tagElement: TagElement): Int {
            return this.indicator - tagElement.indicator
        }
    }


    class TagElementGroup : Comparable<TagElementGroup> {
        var instant: Instant
        var tags: MutableList<TagElement>

        init {
            instant = Instant.now()
            tags = ArrayList()
        }

        override fun compareTo(tagElementGroup: TagElementGroup): Int {
            return this.instant.compareTo(tagElementGroup.instant)
        }
    }

    companion object {

        private val TAG = "DataStorage"


        fun readElementsFromJson(json: JSONObject?): List<TagElementGroup> {
            val result = ArrayList<TagElementGroup>()

            if (json != null) {
                try {
                    val array = json.getJSONArray("data")
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val tags = o.getJSONArray("tags")

                        val group = TagElementGroup()

                        for (j in 0 until tags.length()) {
                            val tag = tags.getJSONObject(j)
                            val element = TagElement()
                            element.id = tag.getString("id")
                            element.name = if (tag.has("name")) tag.getString("name") else ""
                            element.indicator = tag.getInt("indicator")
                            group.tags.add(j, element)

                        }
                        group.instant = Instant.parse(o.getString("date"))

                        result.add(i, group)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
            return result
        }
    }
}
