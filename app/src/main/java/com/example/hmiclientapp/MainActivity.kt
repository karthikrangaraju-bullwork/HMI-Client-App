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
import com.bullwork.hmi_headless.CanMessageAidl
import com.bullwork.hmi_headless.IHmiDataCallback
import com.bullwork.hmi_headless.IHmiDataService
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val TAG = "HMIClientApp"
    private var hmiService: IHmiDataService? = null
    private var isBound = false

    // UI ELEMENTS - VEHICLE & GENERAL
    private lateinit var canIdValue: TextView
    private lateinit var armStateValue: TextView
    private lateinit var imuXValue: TextView
    private lateinit var imuYValue: TextView
    private lateinit var keyStateValue: TextView
    private lateinit var killSwitchValue: TextView

    // UI ELEMENTS - BATTERY (ID 769)
    private lateinit var batterySocValue: TextView
    private lateinit var batterySohValue: TextView
    private lateinit var batteryPowerValue: TextView
    private lateinit var batteryCapacityValue: TextView
    private lateinit var batteryTempValue: TextView

    // UI ELEMENTS - MOTOR 41 (ID 784)
    private lateinit var motorRpmValue: TextView
    private lateinit var motorPowerMValue: TextView
    private lateinit var motorCurrentValue: TextView
    private lateinit var motorTempValue: TextView
    private lateinit var mcuTempValue: TextView

    // UI ELEMENTS - BUTTONS
    private lateinit var insertButton: Button
    private lateinit var updateDbcButton: Button

    // Activity Result Launcher for file picking
    private val dbcFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            readDbcFileContent(uri)
        } else {
            Log.w(TAG, "DBC file selection cancelled by user.")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        canIdValue = findViewById(R.id.can_id_value)
        armStateValue = findViewById(R.id.arm_state_value)
        imuXValue = findViewById(R.id.imu_x_value)
        imuYValue = findViewById(R.id.imu_y_value)

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

        // Initialize buttons and set listeners
        insertButton = findViewById(R.id.button_test_insert)
        updateDbcButton = findViewById(R.id.button_update_dbc)

        insertButton.setOnClickListener { sendDummyData() }
        updateDbcButton.setOnClickListener { startDbcFilePicker() }

        Intent(IHmiDataService::class.java.name).also { intent ->
            // ACTION: Specifies the AIDL interface/action the service is using.
            intent.setAction("com.bullwork.hmi_headless.IHmiDataService")

            // CRITICAL FIX: The package must be the server's application ID: com.bullwork.hmi_headless
            intent.setPackage("com.bullwork.hmi_headless")

            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isBound) registerCallback()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) unregisterCallback()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            hmiService = IHmiDataService.Stub.asInterface(service)
            isBound = true
            Log.d(TAG, "Service Bound: IHmiDataService connected.")
            registerCallback()
            try {
                val latest = hmiService?.getLatestData()
                if (latest != null && latest.messageId != -1) {
                    updateUi(latest)
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to get latest data: ${e.message}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hmiService = null
            isBound = false
            Log.w(TAG, "Service Disconnected.")
        }
    }

    private val hmiCallback = object : IHmiDataCallback.Stub() {
        override fun onNewData(message: CanMessageAidl) {
            runOnUiThread {
                updateUi(message)
            }
        }
    }

    private fun registerCallback() {
        try {
            hmiService?.registerCallback(hmiCallback)
            Log.d(TAG, "Callback registered.")
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to register callback: ${e.message}")
        }
    }

    private fun unregisterCallback() {
        try {
            hmiService?.unregisterCallback(hmiCallback)
            Log.d(TAG, "Callback unregistered.")
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to unregister callback: ${e.message}")
        }
    }

    private fun updateUi(message: CanMessageAidl) {
        val fields = message.fields
        canIdValue.text = message.messageId.toString()

        // Update fields based on CAN ID
        when (message.messageId) {
            785 -> { // Battery Data (ID 769 - No observed change, keeping old fields)
                batterySocValue.text = fields["BATTERY_SOC"] ?: "N/A"
                batterySohValue.text = fields["batterySoh"] ?: "N/A"
                batteryPowerValue.text = fields["batteryPower"] ?: "N/A"
                batteryCapacityValue.text = fields["BATTERY_CAPACITY"] ?: "N/A"
                batteryTempValue.text = fields["batteryTempNtc1"] ?: "N/A"
            }

            // Handles old ID 774 (Ignition)
            774 -> {
                // Check if the DBC was reloaded (look for the test signal name)
                if (fields.containsKey("TEST_KEY_STATE")) {
                    keyStateValue.text = "TEST: ${fields["TEST_KEY_STATE"]}"
                    keyStateValue.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
                } else {
                    keyStateValue.text = fields["Key_On_Off"] ?: "N/A"
                    keyStateValue.setTextColor(ContextCompat.getColor(this, R.color.black))
                }
            }

            // Handles old ID 775 and NEW ID 832 (Vehicle Data & Flags)
            775, 832 -> {
                // Map old 'Key_On_Off' (from 774/775) to new 'MCU_IGNITION' (from 832)
                keyStateValue.text = fields["MCU_IGNITION"] ?: fields["Key_On_Off"] ?: "N/A"

                // Map old 'Arm_state' to new 'ARM_STATE_FLAG' (assuming this is the new field name)
                armStateValue.text = fields["ARM_STATE_FLAG"] ?: fields["Arm_state"] ?: "N/A"

                // Map old 'Kill_switch' to new 'KILL_SWITCH'
                killSwitchValue.text = fields["KILL_SWITCH"] ?: fields["Kill_switch"] ?: "N/A"

                // Keep IMU for backward compatibility, assuming 832 doesn't carry it
                if (message.messageId == 775) {
                    imuXValue.text = fields["imuAngleX"] ?: "N/A"
                    imuYValue.text = fields["imuAngleY"] ?: "N/A"
                }
            }

            // Handles old ID 784 and NEW ID 800 (Motors Data)
            784, 800 -> {
                // Map old 'rpm' to new 'MOTOR_RPM'
                motorRpmValue.text = fields["MOTOR_RPM"] ?: fields["rpm"] ?: "N/A"
                // Map old 'power' to new 'MOTOR_POWER'
                motorPowerMValue.text = fields["MOTOR_POWER"] ?: fields["power"] ?: "N/A"

                // Keep these for backward compatibility/future fields on 800, using "N/A" if not found
                motorCurrentValue.text = fields["phaseCurrent"] ?: "N/A"
                motorTempValue.text = fields["motorTemp"] ?: "N/A"
                mcuTempValue.text = fields["mcuTemp"] ?: "N/A"
            }

            9999 -> { // Inserted Dummy Data
                keyStateValue.text = fields["dummy_key"] ?: "INSERTED"
            }
        }

        // UPDATED LOG STATEMENT to include both keys and values
        Log.d(TAG, "Received message: ID=${message.messageId}, Data=${message.fields.map { "${it.key}=${it.value}" }.joinToString()}")
    }

    private fun sendDummyData() {
        val message = CanMessageAidl(
            id = 0,
            messageId = 9999, // Custom ID for insertion
            data = "DUMMY:12:34:56:78:90:AB:CD:EF",
            timestamp = System.currentTimeMillis(),
            fields = mapOf("dummy_key" to "ACTIVE")
        )
        try {
            hmiService?.insertData(message)
            Log.i(TAG, "Sent dummy data to middleware.")
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to send dummy data: ${e.message}")
        }
    }

    // Launches the system file picker to select a DBC file
    private fun startDbcFilePicker() {
        if (!isBound || hmiService == null) {
            Log.e(TAG, "Cannot start file picker: Service is not bound.")
            return
        }
        updateDbcButton.text = "Selecting File..."
        // Use */* to allow selection of any text/DBC file
        dbcFilePickerLauncher.launch("*/*")
    }

    // Reads the content of the selected file asynchronously
    private fun readDbcFileContent(uri: android.net.Uri) {
        // Read file content off the main thread
        Executors.newSingleThreadExecutor().execute {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()

                    if (content.isNotBlank()) {
                        runOnUiThread {
                            updateDbcButton.text = "File Read. Sending to Service..."
                        }
                        sendDbcContentToService(content)
                    } else {
                        Log.e(TAG, "Selected file is empty.")
                        runOnUiThread {
                            updateDbcButton.text = "DBC Update FAILED (File Empty)"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading selected DBC file: ${e.message}", e)
                runOnUiThread {
                    updateDbcButton.text = "DBC Update FAILED (Read Error)"
                }
            }
        }
    }

    // Sends the actual DBC file content to the service
    private fun sendDbcContentToService(dbcContent: String) {
        try {
            val success = hmiService?.reloadDbc(dbcContent)
            runOnUiThread {
                if (success == true) {
                    Log.i(TAG, "DBC RELOAD SUCCESS. Middleware rules updated with uploaded file.")
                    updateDbcButton.text = "DBC RELOADED! (Uploaded File Active)"
                    updateDbcButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.teal_700)
                } else {
                    Log.e(TAG, "DBC RELOAD FAILED! Check middleware service logs for parsing errors.")
                    updateDbcButton.text = "DBC RELOAD FAILED (Service Error)"
                }
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "AIDL call to reloadDbc failed: ${e.message}", e)
            runOnUiThread {
                updateDbcButton.text = "DBC RELOAD AIDL ERROR"
            }
        }
    }
}