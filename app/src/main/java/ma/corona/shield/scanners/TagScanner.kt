package ma.corona.shield.scanners

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import ma.corona.shield.db.DataStorage

import java.time.Instant


open class TagScanner(ctx: Context) {

    val context = ctx

    var thread: Thread

    var dataStorage: DataStorage
        protected set

    protected var tagsGroup: DataStorage.TagElementGroup

    var isScanning = false
        private set


    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            Log.v(TAG, "Scan receiver")
            handleReceivedMessage(intent)

        }
    }

    protected open val filters: Array<String>?
        get() = null

    open val storagePrefix: String
        get() = "SS"

    init {
        Log.v(TAG, " Tag Scanner initialized")
        tagsGroup = DataStorage.TagElementGroup()
        dataStorage = DataStorage(context, storagePrefix)
        thread = Thread(ProcessScanning())

    }

    private inner class ProcessScanning : Runnable {
        override fun run() {
            while (!Thread.interrupted()) {
                try {
                    Log.v(TAG, "Process scanning iteration")
                    //if (!isScanning)
                        startScanning()
                    Thread.sleep((SCAN_PERIOD_SEC * 1000).toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }

    open fun start() {
        Log.v(TAG, "Tag scanner starting")
        val intentFilter = IntentFilter()
        for (filter in filters!!) {
            intentFilter.addAction(filter)
        }
        context.registerReceiver(scanReceiver, intentFilter)
        thread.start()
    }


    fun stop() {
        Log.v(TAG, "Tag scanner stopping")
        context.unregisterReceiver(scanReceiver)
        thread.interrupt()
    }


    protected open fun handleReceivedMessage(intent: Intent) {

    }

    open fun startScanning() {
        tagsGroup.tags.clear()
        tagsGroup.instant = Instant.now()
        isScanning = true
    }

    open fun stopScanning() {
        isScanning = false
    }

    companion object {

        private val TAG = "ScannerService"
        private const val SCAN_PERIOD_SEC = 30
    }
}
