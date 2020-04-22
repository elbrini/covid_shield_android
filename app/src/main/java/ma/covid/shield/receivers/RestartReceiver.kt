package ma.covid.shield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import ma.covid.shield.services.TagsManagerService

class RestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val TAG = "RestartReceiver"
        Log.v(TAG, "Restart services")
        Toast.makeText(context, "Intent Detected.", Toast.LENGTH_LONG).show();
        context.startService(Intent(context, TagsManagerService::class.java))
    }
}
