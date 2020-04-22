package ma.covid.shield.scanners

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log

import ma.covid.shield.db.DataStorage

import java.time.Instant
import java.util.ArrayList
import java.util.Collections

import android.bluetooth.BluetoothClass.Device.*
import android.bluetooth.BluetoothClass.Device.Major.UNCATEGORIZED
import android.content.Context
import java.lang.Math.min

class BluetoothTagScanner(ctx: Context) : TagScanner(ctx) {

    private val adapter: BluetoothAdapter

    private val devices: MutableList<BluetoothDevice>

    init {
        adapter = BluetoothAdapter.getDefaultAdapter()
        devices = ArrayList()
    }


    override val filters: Array<String>?
        get() {
            Log.v(TAG, "Get Filters")
            return arrayOf(BluetoothDevice.ACTION_FOUND, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

    override val storagePrefix: String
        get() = "BL"

    override fun handleReceivedMessage(intent: Intent) {
        val action = intent.action
        Log.v(TAG, "Action:" + action!!)
        if (BluetoothDevice.ACTION_FOUND == action) {
            // Discovery has found a device. Get the BluetoothDevice
            // object and its info from the Intent.
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, java.lang.Short.MIN_VALUE)
            val deviceName = device!!.name
            val deviceHardwareAddress = device.address // MAC address
            val deviceClass = device.bluetoothClass.deviceClass
            Log.v(
                TAG,
                "Device discovered: $deviceName, mac:$deviceHardwareAddress class: $deviceClass rssi:$rssi"
            )
            if (isFixedDevice(device) && !devices.contains(device)) {
                if (tagsGroup.tags.isEmpty())
                    tagsGroup.instant = Instant.now()


                Log.v(TAG, "Devices is not portable: $deviceClass")
                devices.add(device)

                val element = DataStorage.TagElement()
                element.id = deviceHardwareAddress
                element.name = deviceName
                element.indicator = rssi.toInt()
                tagsGroup.tags.add(element)

            }

        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
            if (isScanning) {
                Log.v(TAG, "Devices count:" + devices.size)
                stopScanning()
                Collections.sort<DataStorage.TagElement>(tagsGroup.tags, Collections.reverseOrder<Any>())
                tagsGroup.tags = tagsGroup.tags.subList(0, min(tagsGroup.tags.size, 3))
                dataStorage.saveElements(tagsGroup, true)
            }

        }
    }

    private fun isFixedDevice(device: BluetoothDevice): Boolean {
        var res = true
        when (device.bluetoothClass.deviceClass) {
            AUDIO_VIDEO_HIFI_AUDIO, AUDIO_VIDEO_SET_TOP_BOX, AUDIO_VIDEO_UNCATEGORIZED, AUDIO_VIDEO_VCR, AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER, AUDIO_VIDEO_VIDEO_CONFERENCING, AUDIO_VIDEO_VIDEO_MONITOR, COMPUTER_DESKTOP, COMPUTER_SERVER, COMPUTER_UNCATEGORIZED, HEALTH_UNCATEGORIZED, HEALTH_WEIGHING, PHONE_MODEM_OR_GATEWAY, PHONE_UNCATEGORIZED, UNCATEGORIZED -> res =
                true

            else -> {
            }
        }
        return res
    }

    override fun startScanning() {
        if (isScanning)
            stopScanning()

        devices.clear()
        tagsGroup.tags.clear()
        tagsGroup.instant = Instant.now()

        if(adapter.isEnabled && adapter.startDiscovery())
            super.startScanning()


    }

    override fun stopScanning() {
        super.stopScanning()
        adapter.cancelDiscovery()
    }

    companion object {

        private val TAG = "BluetoothScannerService"
    }

}
