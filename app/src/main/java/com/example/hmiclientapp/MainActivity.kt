package com.example.hmiclientapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myhmimiddlewareapplication.CanMessageAidl
import com.example.myhmimiddlewareapplication.IHmiDataCallback
import com.example.myhmimiddlewareapplication.IHmiDataService
import java.util.Random
import android.graphics.Typeface // NEW
import androidx.core.content.ContextCompat // NEW

class MainActivity : AppCompatActivity() {

    private val TAG = "HMIClientApp"
    private var hmiService: IHmiDataService? = null
    private var isBound = false

    // UI ELEMENTS - VEHICLE & GENERAL
    private lateinit var canIdValue: TextView
    private lateinit var armStateValue: TextView
    private lateinit var imuXValue: TextView
    private lateinit var imuYValue: TextView
    private lateinit var keyStateValue: TextView        // ADDED
    private lateinit var killSwitchValue: TextView      // ADDED

    // UI ELEMENTS - BATTERY (ID 769)
    private lateinit var batterySocValue: TextView
    private lateinit var batterySohValue: TextView
    private lateinit var batteryPowerValue: TextView
    private lateinit var batteryCapacityValue: TextView // ADDED
    private lateinit var batteryTempValue: TextView     // ADDED

    // UI ELEMENTS - MOTOR 41 (ID 784)
    private lateinit var motorRpmValue: TextView        // ADDED
    private lateinit var motorPowerMValue: TextView     // ADDED
    private lateinit var motorCurrentValue: TextView    // ADDED
    private lateinit var motorTempValue: TextView       // ADDED
    private lateinit var mcuTempValue: TextView         // ADDED


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        canIdValue = findViewById(R.id.can_id_value)
        armStateValue = findViewById(R.id.arm_state_value)
        imuXValue = findViewById(R.id.imu_x_value)
        imuYValue = findViewById(R.id.imu_y_value)

        // Initialize new UI components
        keyStateValue = findViewById(R.id.key_state_value)
        killSwitchValue = findViewById(R.id.kill_switch_value)

        batterySocValue = findViewById(R.id.battery_soc_value)
        batterySohValue = findViewById(R.id.battery_soh_value)
        batteryPowerValue = findViewById(R.id.battery_power_value)
        batteryCapacityValue = findViewById(R.id.battery_capacity_value)
        batteryTempValue = findViewById(R.id.battery_temp_value)

        motorRpmValue = findViewById(R.id.motor_rpm_value)
        motorPowerMValue = findViewById(R.id.motor_power_m_value)
        motorCurrentValue = findViewById(R.id.motor_current_value)
        motorTempValue = findViewById(R.id.motor_temp_value)
        mcuTempValue = findViewById(R.id.mcu_temp_value)

        // Bind to the service
        Intent(IHmiDataService::class.java.name).also { intent ->
            // Must set package explicitly for implicit intents in AIDL across apps
            intent.setPackage("com.example.myhmimiddlewareapplication")
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        findViewById<Button>(R.id.button_test_insert).setOnClickListener {
            sendDummyData()
        }
    }

    // AIDL SERVICE CONNECTION
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            hmiService = IHmiDataService.Stub.asInterface(service)
            isBound = true
            try {
                hmiService?.registerCallback(hmiCallback)
                Log.i(TAG, "Service bound and callback registered.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register callback", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hmiService = null
            isBound = false
            Log.w(TAG, "Service disconnected.")
        }
    }

    // AIDL CALLBACK IMPLEMENTATION
    private val hmiCallback = object : IHmiDataCallback.Stub() {
        override fun onNewData(message: CanMessageAidl) {
            updateUi(message)
        }
    }

    override fun onDestroy() {
        if (isBound) {
            try {
                hmiService?.unregisterCallback(hmiCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister callback", e)
            }
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    private fun setStatusText(textView: TextView, status: String?) {
        textView.text = status ?: "N/A"
        val context = textView.context
        // Define the active (green) and inactive (red/default) colors
        val activeColor = ContextCompat.getColor(context, R.color.teal_700) // Assuming green/teal is good
        val inactiveColor = ContextCompat.getColor(context, R.color.black)

        // Check for "Active" or "ON" status keywords
        val isActive = status.equals("Active", ignoreCase = true) || status.equals("ON", ignoreCase = true) || status == "1"

        textView.setTextColor(if (isActive) activeColor else inactiveColor)
        textView.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
    }

    /**
     * Updates the UI based on the incoming CAN message.
     */
    private fun updateUi(message: CanMessageAidl) {
        runOnUiThread {
            canIdValue.text = message.messageId.toString()

            val fields = message.fields
            val canId = message.messageId

            // Update specific sections based on the CAN ID received
            when (canId) {
                774 -> {
                    // Ignition Data
                    setStatusText(keyStateValue, fields["Key_On_Off"])
                }
                769 -> {
                    // Battery Data 1
                    batterySocValue.text = fields["batterySoc"] ?: "N/A"
                    batterySohValue.text = fields["batterySoh"] ?: "N/A"
                    batteryPowerValue.text = fields["batteryPower"] ?: "N/A"
                    batteryCapacityValue.text = fields["batteryCapacity"] ?: "N/A"
                    batteryTempValue.text = fields["batteryTempNtc1"] ?: "N/A"
                }
                775 -> {
                    // Vehicle State / IMU
                    setStatusText(armStateValue, fields["Arm_state"])
                    setStatusText(killSwitchValue, fields["Kill_switch"])
                    imuXValue.text = fields["imuAngleX"] ?: "N/A"
                    imuYValue.text = fields["imuAngleY"] ?: "N/A"
                }
                784 -> {
                    // Motor 41 Performance
                    motorRpmValue.text = fields["rpm"] ?: "N/A"
                    motorPowerMValue.text = fields["power"] ?: "N/A"
                    motorCurrentValue.text = fields["phaseCurrent"] ?: "N/A"
                    motorTempValue.text = fields["motorTemp"] ?: "N/A"
                    mcuTempValue.text = fields["mcuTemp"] ?: "N/A"
                }
            }
        }
    }
    /**
     * Sends a dummy message (CAN ID 9999) to the middleware service via AIDL insertData().
     */
    private fun sendDummyData() {
        if (!isBound || hmiService == null) {
            Log.e(TAG, "Service not bound. Cannot send data.")
            return
        }

        val dummyData = CanMessageAidl(
            id = 0,
            messageId = 9999,
            data = "Test Data from Client App",
            timestamp = System.currentTimeMillis(),
            fields = mapOf(
                "Test_Field_1" to "123",
                "Status_Flag" to (if (Random().nextBoolean()) "Active" else "Inactive")
            )
        )

        try {
            hmiService?.insertData(dummyData)
            Log.d(TAG, "Sent dummy CAN ID 9999 to service.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert dummy data: ${e.message}", e)
        }
    }
}