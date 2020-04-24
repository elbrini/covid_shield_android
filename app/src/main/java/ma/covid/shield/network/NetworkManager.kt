package ma.covid.shield.network

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

import ma.covid.shield.db.DataStorage

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.time.Instant


class NetworkManager private constructor(context: Context) {

    private val worker: NetworkWorker
    private val thread: Thread
    private val sharedPreferences: SharedPreferences
    private var token: Int = 0
    private var listener: DataListener? = null


    init {
        this.listener = listener
        this.worker = NetworkWorker(context)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        token = sharedPreferences.getInt(TOKEN_KEY, -1)

        thread = Thread(ProcessFetching())
        thread.start()


    }

    fun registerListener(listener: DataListener) {
        this.listener = listener
    }

    fun uploadTags(
        storageList: List<DataStorage>,
        start: Instant,
        end: Instant,
        callback: NetworkCallback
    ): Boolean {
        var result = true
        val json = JSONObject()


        for (storage in storageList) {
            try {
                val data = JSONArray()
                var instant = start
                while (instant.isBefore(end) || instant == end) {
                    val o = storage.getSavedJson(instant)
                    if (o != null)
                        data.put(o)
                    instant = instant.plusSeconds(3600)
                }
                json.put(storage.prefix, data)

            } catch (e: JSONException) {
                e.printStackTrace()
                result = false
                break
            }

        }

        if (result)
            worker.upload(json, callback)


        return result
    }


    fun fetchTags(callback: NetworkCallback): Boolean {
        val result = false
        worker.fetch(callback, token)
        return result
    }

    private fun saveToken(token: Int) {
        this.token = token
        val editor = sharedPreferences.edit()
        editor.putInt(TOKEN_KEY, token)
        editor.commit()
    }


    private inner class ProcessFetching : Runnable {
        override fun run() {
            while (!Thread.interrupted()) {
                try {
                    fetchTags(object : NetworkManager.NetworkCallback {
                        override fun onResponse(response: JSONObject) {
                            Log.v(TAG, "Response: $response")
                            try {
                                saveToken(response.getInt("token"))
                                if (listener != null && !response.isNull("response"))
                                    listener!!.onReceived(response.getJSONObject("response"))
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                        }

                        override fun onError() {

                        }
                    })
                    Thread.sleep((CHECK_PERIOD_SEC * 1000).toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }

    interface NetworkCallback {
        fun onResponse(response: JSONObject)

        fun onError()
    }

    interface DataListener {
        fun onReceived(json: JSONObject)
    }

    companion object {

        private const val TAG = "NetworkManager"
        private const val TOKEN_KEY = "net_token"
        private const val CHECK_PERIOD_SEC = 120

        private var instance: NetworkManager? = null

        fun getInstance(context: Context): NetworkManager {
            if (instance == null)
                instance = NetworkManager(context)

            return instance!!
        }
    }


}
