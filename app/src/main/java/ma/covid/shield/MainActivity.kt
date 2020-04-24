package ma.covid.shield

import androidx.appcompat.app.AppCompatActivity

import android.Manifest

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.ImageView

import android.preference.PreferenceManager
import android.util.Log
import android.view.View


import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Handler
import android.view.View.GONE
import android.webkit.RenderProcessGoneDetail
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_layout.*
import kotlinx.android.synthetic.main.splash_layout.*
import ma.covid.shield.activities.HowToActivity
import ma.covid.shield.activities.SettingsActivity
import ma.covid.shield.services.TagsManagerService


@RuntimePermissions
class MainActivity : AppCompatActivity() {


    private var bluetoothButton: ImageView? = null
    private var wifiButton: ImageView? = null

    private var sharedPreferences: SharedPreferences? = null



    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            if (bundle != null) {
                if(bundle.getInt(TagsManagerService.EVENT_KEY) == TagsManagerService.EVENT_PROFILE_UPDATED)
                {
                    if(getUserProfile() < 1)
                        setUserProfile(1)
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)



        Thread(Runnable {
            Thread.sleep(2000)
            runOnUiThread {
                splash.visibility = View.GONE
                mainView.visibility = View.VISIBLE
            }
        }).start()




        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)


        positiveButton.setOnClickListener {
            uploadData()
            setUserProfile(2)
        }

        bluetoothButton = findViewById(R.id.bluetoothButton)
        bluetoothButton!!.setOnClickListener {
            val enable = !bluetoothEnabled()
            if(enableBluetooth(enable))
                    (it as ImageView).setImageResource(if (enable) R.mipmap.bluetooth_blue else (R.mipmap.bluetooth_white))
            Handler().postDelayed( {updateProtectionStatus()}, 1000)

        }

        wifiButton = findViewById(R.id.wifiButton)
        wifiButton!!.setOnClickListener {
            val enable = !wifiEnabled()
            if(enableWifi(enable))
                (it as ImageView).setImageResource(if (enable) R.mipmap.wifi_blue else (R.mipmap.wifi_white))
            Handler().postDelayed( {updateProtectionStatus()}, 1000)

        }

        settingsButton.setOnClickListener {

            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)

        }

        protectionStatus.setOnClickListener {

            val intent = Intent(this, HowToActivity::class.java)
            startActivity(intent)
        }


        var intent = Intent(this, TagsManagerService::class.java)
        startService(intent)


    }

    override fun onResume() {
        registerReceiver(receiver, IntentFilter(
                TagsManagerService.NOTIFICATION))

        bluetoothButton?.setImageResource(
            if (bluetoothEnabled())
                R.mipmap.bluetooth_blue
            else R.mipmap.bluetooth_white)
        wifiButton?.setImageResource(
            if (wifiEnabled())
                R.mipmap.wifi_blue
            else R.mipmap.wifi_white)

        updateProtectionStatus()

        updateProfileStatus()

        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    /******* TAGS STUFF  *****/
    fun uploadData() {

        val intent = Intent(this, TagsManagerService::class.java).apply {
            putExtra(TagsManagerService.CMD_KEY, TagsManagerService.CMD_UPLOAD_DATA)
        }
        startService(intent)
    }

    /******* BLUETOOTH STUFF *****/
    internal fun bluetoothEnabled(): Boolean {
        return sharedPreferences!!.getBoolean("bluetooth_enabled", false)

    }

    fun enableBluetooth(activate: Boolean): Boolean {

        if(activate) {
            BluetoothAdapter.getDefaultAdapter().enable()
            sharedPreferences!!.edit().putBoolean("bluetooth_enabled", true).commit()
        }
        else
        {
            sharedPreferences!!.edit().putBoolean("bluetooth_enabled", false).commit()
        }
        return true
    }

    /******* WIFI STUFF *****/
    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun activateWifiPermission() {
        Log.v(TAG, "Wifi activated")
    }


    fun wifiEnabled(): Boolean {
        return sharedPreferences!!.getBoolean("wifi_enabled", false)

    }

    fun enableWifi(activate: Boolean): Boolean {
        if(activate)
        {
            showWifiAlert()
        }
        else
        {
            doEnableWifi(false)
        }
        return true
    }

    fun doEnableWifi(activate: Boolean) {
        if(activate) {
            activateWifiPermissionWithPermissionCheck()
            (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled = true
            sharedPreferences!!.edit().putBoolean("wifi_enabled", true).commit()
        }
        else
        {
            sharedPreferences!!.edit().putBoolean("wifi_enabled", false).commit()
        }
    }

    fun showWifiAlert()
    {
        val alerted = sharedPreferences!!.getBoolean("wifi_alert", false)
        if (!alerted) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.alert_title))
                .setMessage(getString(R.string.wifi_location_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.wifi_understood)) { dialog, id ->
                    Log.v(TAG, getString(R.string.wifi_understood))
                    sharedPreferences!!.edit().putBoolean("wifi_alert", true).commit()
                    doEnableWifi(true)
                }
                .setNegativeButton(getString(R.string.wifi_cancel)) { dialog, id -> dialog.dismiss()}
            val disc = builder.create()
            disc.show()

        }
        else
            doEnableWifi(true)

    }

    /******* PROTECTION STATUS *****/
    fun wifiProtected(): Boolean
    {
        return wifiEnabled() &&
                (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled &&
                (applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    fun bluetoothProtected(): Boolean
    {
        return  bluetoothEnabled() && BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    fun wifiMismatch(): Boolean
    {
        return wifiEnabled() &&
                !((applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled &&
                (applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    fun bluetoothMismatch(): Boolean
    {
        return bluetoothEnabled() && !BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    fun getProtectionLevel(): Int
    {
        var res = 0
        if(bluetoothProtected()) res++
        if(wifiProtected()) res++
        return res
    }

    fun updateProtectionStatus()
    {
        val level = getProtectionLevel()

        protectionStatus.setImageDrawable(
            if(bluetoothMismatch() || wifiMismatch())
                getDrawable(R.mipmap.shield_yellow)
            else if(level == 0)
                getDrawable(R.mipmap.shield_red)
            else if( level == 1)
                getDrawable(R.mipmap.shield_yellow)
            else
                getDrawable(R.mipmap.shield_green)
        )
        val progress = 50 * level
        protectionLevel.text = "$progress%"
    }

    /******* USER STATUS *****/

    fun getUserProfile(): Int
    {
        return sharedPreferences!!.getInt("user_profile", 0)

    }

    fun setUserProfile(level: Int)
    {
        sharedPreferences!!.edit().putInt("user_profile", level).commit()

        updateProfileStatus()

    }

    fun updateProfileStatus()
    {
        val level = getUserProfile()
        userStatus.setImageDrawable(
            when (level) {
                0 -> getDrawable(R.mipmap.user_green)
                1 -> getDrawable(R.mipmap.user_yellow)
                else -> getDrawable(R.mipmap.user_red)
            }
        )
        positiveButton.visibility = if(level == 2) View.GONE else View.VISIBLE

        userProfile.text =
            when (level) {
                0 -> getString(R.string.sain)
                1 -> getString(R.string.suspect)
                else -> getString(R.string.atteint)
            }
        coronavirusStatus.setImageDrawable(
            when (level) {
                0 -> getDrawable(R.mipmap.coronavirus_sad)
                1 -> getDrawable(R.mipmap.coronavirus_neutral)
                else -> getDrawable(R.mipmap.coronavirus_happy)
            }
        )

    }




    companion object {


        private val TAG = "MainActivity"
    }


}
