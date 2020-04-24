package ma.covid.shield.network

import android.content.Context
import android.util.Log


import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest

import org.json.JSONObject

class NetworkWorker(internal var ctx: Context) {
    internal var requestQueue: RequestQueue


    init {

        val cache = DiskBasedCache(ctx.cacheDir, 1024 * 1024) // 1MB cap

        val network = BasicNetwork(HurlStack())

        requestQueue = RequestQueue(cache, network)

        requestQueue.start()

    }


    fun upload(json: JSONObject, callback: NetworkManager.NetworkCallback) {

        val request = JsonObjectRequest(
            Request.Method.POST,
            "$SERVER_URL/tags",
            json,
            Response.Listener { response -> callback.onResponse(response) },
            Response.ErrorListener { error ->
                Log.v(TAG, "Error: " + error.message)
                callback.onError()
            })
        request.tag = "upload"
        requestQueue.add(request)

    }

    fun fetch(callback: NetworkManager.NetworkCallback, token: Int) {
        val request = JsonObjectRequest(
            Request.Method.GET,
            "$SERVER_URL/tags?token=$token",
            null,
            Response.Listener { response -> callback.onResponse(response) },
            Response.ErrorListener { callback.onError() })
        request.tag = "fetch"
        requestQueue.add(request)


    }

    companion object {

        //val SERVER_URL = "http://3.120.149.197:1234"
        val SERVER_URL = "http://192.168.1.4:1234"
        private val TAG = "NetworkWorker"
    }


}
