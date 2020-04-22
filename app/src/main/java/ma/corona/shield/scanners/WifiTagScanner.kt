package ma.corona.shield.scanners

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log

import ma.corona.shield.db.DataStorage

import java.time.Instant
import java.util.Collections

import java.lang.Math.min
import java.util.logging.Handler
import android.os.Looper
import android.R.attr.name



class WifiTagScanner(ctx: Context) : TagScanner(ctx) {
    private var wifiManager: WifiManager? = null

    override val filters: Array<String>?
        get() {
            Log.v(TAG, "Get Filters")
            return arrayOf(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,
                WifiManager.RSSI_CHANGED_ACTION)
        }

    override val storagePrefix: String
        get() = "WIFI"


    override fun start() {
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        super.start()
    }

    override fun handleReceivedMessage(intent: Intent) {
        Log.v(TAG, "Gotcha: " + intent.action)

        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION ||
                intent.action == WifiManager.RSSI_CHANGED_ACTION) {

            if (!isScanning)
                return

            val results = wifiManager!!.scanResults
            Log.v(TAG, "Scan Results")
            val iterator = results.iterator()
            Log.v(TAG, "Total APs found: " + results.size)

            for (scanResult in results) {
                Log.v(TAG, " SSID: " + scanResult.SSID + " - " + scanResult.capabilities)

            }
            tagsGroup.instant = Instant.now()

            while (iterator.hasNext()) {
                val next = iterator.next()
                val bssid = next.BSSID // MAC address
                val ssid = next.SSID // AP name
                val rssi = next.level // Received Signal Strength Indicator
                Log.v(TAG, "BSSID: " + next.BSSID + " SSID: " + ssid + " RSSI: " + rssi)

                val element = DataStorage.TagElement()
                element.id = bssid
                element.name = ssid
                element.indicator = rssi
                tagsGroup.tags.add(element)
            }
            Collections.sort<DataStorage.TagElement>(tagsGroup.tags, Collections.reverseOrder<Any>())
            tagsGroup.tags = tagsGroup.tags.subList(0, min(tagsGroup.tags.size, 3))
            dataStorage.saveElements(tagsGroup, true)

            stopScanning()

        }
    }

    override fun startScanning() {
        Log.v(TAG, "Start scanning")

        if (isScanning)
            stopScanning()

        if(wifiManager!!.isWifiEnabled ) {
            wifiManager!!.isWifiEnabled = true // Throttle work around
            wifiManager!!.startScan()
            Log.v(TAG, "Wifi scan started!")
            super.startScanning()
        }

    }

    override fun stopScanning() {
        super.stopScanning()
    }

    companion object {

        private val TAG = "WifiTagScanner"
    }

}
