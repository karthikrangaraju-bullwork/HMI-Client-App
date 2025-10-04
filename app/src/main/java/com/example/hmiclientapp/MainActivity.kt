package com.example.hmiclientapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myhmimiddlewareapplication.CanMessageAidl
import com.example.myhmimiddlewareapplication.IHmiDataCallback
import com.example.myhmimiddlewareapplication.IHmiDataService

class MainActivity : AppCompatActivity() {

    private val TAG = "HmiClient"

    // UI elements
    private lateinit var statusTextView: TextView
    private lateinit var canIdValue: TextView
    private lateinit var batterySocValue: TextView
    private lateinit var batterySohValue: TextView
    private lateinit var batteryPowerValue: TextView
    private lateinit var armStateValue: TextView
    private lateinit var imuXValue: TextView
    private lateinit var imuYValue: TextView
    private lateinit var insertButton: Button

    // AIDL service
    private var hmiService: IHmiDataService? = null
    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.status_text)
        canIdValue = findViewById(R.id.can_id_value)
        batterySocValue = findViewById(R.id.battery_soc_value)
        batterySohValue = findViewById(R.id.battery_soh_value)
        batteryPowerValue = findViewById(R.id.battery_power_value)
        armStateValue = findViewById(R.id.arm_state_value)
        imuXValue = findViewById(R.id.imu_x_value)
        imuYValue = findViewById(R.id.imu_y_value)
        insertButton = findViewById(R.id.insert_button)

        insertButton.setOnClickListener { sendDummyData() }
    }

    override fun onStart() {
        super.onStart()
        bindToHmiService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromHmiService()
    }

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            hmiService = IHmiDataService.Stub.asInterface(service)
            isBound = true
            Log.i(TAG, "Service connected")
            statusTextView.text = "Status: Connected"
            registerCallback()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hmiService = null
            isBound = false
            statusTextView.text = "Status: Disconnected"
        }
    }

    private fun bindToHmiService() {
        if (isBound) return

        val intent = Intent().apply {
            component = ComponentName(
                "com.example.myhmimiddlewareapplication",
                "com.example.myhmimiddlewareapplication.MainService"
            )
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        statusTextView.text = "Status: Binding..."
    }

    private fun unbindFromHmiService() {
        if (!isBound) return
        try {
            hmiService?.unregisterCallback(hmiCallback)
        } catch (e: RemoteException) { }
        finally {
            unbindService(serviceConnection)
            hmiService = null
            isBound = false
            statusTextView.text = "Status: Unbound"
        }
    }

    private fun registerCallback() {
        try {
            hmiService?.registerCallback(hmiCallback)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register callback", e)
        }
    }

    // AIDL callback
    private val hmiCallback = object : IHmiDataCallback.Stub() {
        override fun onNewData(message: CanMessageAidl) {
            runOnUiThread { updateUi(message) }
        }
    }

    private fun updateUi(message: CanMessageAidl) {
        val canId = message.messageId
        // Retrieve structured fields map directly
        val fields = message.fields

        // Reset all specific values to N/A or initial label, ensuring they don't hold stale data.
        canIdValue.text = "ID: $canId"
        batterySocValue.text = "Battery SOC: N/A"
        batterySohValue.text = "Battery SOH: N/A"
        batteryPowerValue.text = "Battery Power: N/A"
        armStateValue.text = "Arm State: N/A"
        imuXValue.text = "IMU X: N/A"
        imuYValue.text = "IMU Y: N/A"

        when (canId) {
            769 -> {
                // Update with full formatted string: "Label: Value"
                batterySocValue.text = "Battery SOC: ${fields["batterySoc"] ?: "N/A"}"
                batterySohValue.text = "Battery SOH: ${fields["batterySoh"] ?: "N/A"}"
                batteryPowerValue.text = "Battery Power: ${fields["batteryPower"] ?: "N/A"}"
            }
            775 -> {
                // Update with full formatted string: "Label: Value"
                armStateValue.text = "Arm State: ${fields["Arm_state"] ?: "N/A"}"
                imuXValue.text = "IMU X: ${fields["imuAngleX"] ?: "N/A"}"
                imuYValue.text = "IMU Y: ${fields["imuAngleY"] ?: "N/A"}"
            }
        }

        // Update main status bar using the newly set text values
        val socText = fields["batterySoc"] ?: "N/A"
        val armText = fields["Arm_state"] ?: "N/A"

        statusTextView.text = buildString {
            append("Status: LIVE | CAN ID: $canId")
            if (canId == 769) append(" | SOC: $socText")
            if (canId == 775) append(" | ARM: $armText")
        }
    }

    private fun sendDummyData() {
        if (!isBound) return

        // UPDATED: Sending dummy structured data (CAN ID 9999 for test)
        val dummyFields = mapOf(
            "Test Signal" to "42",
            "Status" to "OK",
            "Time" to System.currentTimeMillis().toString()
        )

        val dummy = CanMessageAidl(
            id = 0,
            messageId = 9999,
            // Keeping 'data' simple or empty since we now use 'fields'
            data = "Dummy Data Sent",
            timestamp = System.currentTimeMillis(),
            fields = dummyFields
        )

        try {
            hmiService?.insertData(dummy)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to send dummy data", e)
        }
    }
}
