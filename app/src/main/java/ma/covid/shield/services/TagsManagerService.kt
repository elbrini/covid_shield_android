package ma.covid.shield.services

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import ma.covid.shield.MatchScorer
import ma.covid.shield.TagsManager
import ma.covid.shield.db.DataStorage
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import ma.covid.shield.MainActivity
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import ma.covid.shield.R
import ma.covid.shield.scanners.BluetoothTagScanner
import ma.covid.shield.scanners.TagScanner
import ma.covid.shield.scanners.WifiTagScanner


class TagsManagerService : Service() {


    val TAG = "TagsManagerService"
    val CHANNEL_ID = "TagsManagerServiceChannel"


    private var tagsManager: TagsManager? = null
    private val scanners = ArrayList<TagScanner>()


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            if (bundle != null) {


            }
        }
    }
    private val tagsListener = object : TagsManager.TagsManagerListener {

        override fun onMatchCandidate(json: JSONObject) {

            val storageList = ArrayList<DataStorage>()
            for (scanner in scanners)
                storageList.add(scanner.dataStorage)
            val score = tagsManager!!.match(storageList, json)
            Log.v(TAG, "SCORE: $score")

            if (MatchScorer.judgeScore(score, applicationContext))
                notifyStatusChange()

        }

        override fun onUpload(success: Boolean) {
            Log.v(TAG, "Upload result: $success")

        }
    }

    private fun uploadData() {
        val storageList = ArrayList<DataStorage>()
        for (scanner in scanners)
            storageList.add(scanner.dataStorage)

        val end = Instant.now()
        val start = end.minus(21, ChronoUnit.DAYS)
        tagsManager!!.uploadTags(storageList, start, end)
    }


    fun notifyStatusChange() {
        notifyEvent(EVENT_PROFILE_UPDATED, true)
    }

    private fun notifyEvent(event: Int, result: Boolean) {

        showEventNotification()

        val intent = Intent(NOTIFICATION)
        intent.putExtra(EVENT_KEY, event)
        intent.putExtra(RESULT_KEY, result)
        sendBroadcast(intent)
    }

    override fun onCreate() {

        Log.v(TAG, "OnCreate")

        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_content))
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .build()


        startForeground(1, notification)

        tagsManager = TagsManager(this, tagsListener)

        val bluetoothScanner = BluetoothTagScanner(this)
        bluetoothScanner.start()
        scanners.add(bluetoothScanner)

        val wifiScanner = WifiTagScanner(this)
        wifiScanner.start()
        scanners.add(wifiScanner)

        super.onCreate()


    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.hasExtra(CMD_KEY)) {
            when (intent.extras?.getInt(CMD_KEY)) {
                CMD_UPLOAD_DATA -> {
                    Log.v(TAG, "Upload data& command")
                    uploadData()
                }
                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Tags manager service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun showEventNotification()
    {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        var notif = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.shield_orange)
        .setContentTitle(getString(R.string.notif_title))
        .setContentText(getString(R.string.suspect_notif_content))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.suspect_notif_content)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .build()

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1234, notif)
        }

    }


    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        Log.v(TAG, "tags manager destroyed")
        Toast.makeText(this, "tags manager service stopped", Toast.LENGTH_SHORT).show()
        stopScanners()
        super.onDestroy()
    }


    fun stopScanners()
    {
        for(scanner in scanners)
        {
            scanner.stop()
        }
    }

    companion object {


        val NOTIFICATION = "ma.covid.shield.service.receiver"

        val CMD_KEY = "TagsManagerServiceCmd"
        val CMD_UPLOAD_DATA = 4000



        val EVENT_KEY = "TagsManagerServiceEvent"
        val RESULT_KEY = "TagsManagerServiceResult"
        val EVENT_PROFILE_UPDATED = 5000


    }


}
