package com.rvp.callermonitor.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rvp.callermonitor.R
import com.rvp.callermonitor.model.CallerInfo
import com.rvp.callermonitor.receiver.CallReceiver
import com.rvp.callermonitor.viewmodel.CallViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.telephony.TelephonyManager
import android.widget.Button
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.google.android.material.appbar.MaterialToolbar
import android.util.Log
import com.rvp.callermonitor.ui.CallMonitoringService
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.rvp.callermonitor.model.UnknownCallLog

/**
 * Main activity that observes call events and shows notifications for unknown callers.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val callViewModel: CallViewModel by viewModels()
    private lateinit var incomingCallReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up MaterialButton for viewing logs
        val btnViewLogs = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewLogs)
        btnViewLogs.setOnClickListener {
            Log.d("MainActivity", "View Logs button clicked")
            val intent = Intent(this, UnknownCallLogsActivity::class.java)
            startActivity(intent)
        }
        
        // Set up test notification button
        val btnTestNotification = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTestNotification)
        btnTestNotification.setOnClickListener {
            Log.d("MainActivity", "Test notification button clicked")
            // Simulate an incoming call from an unknown number
            callViewModel.onIncomingCall(getString(R.string.test_phone_number_1))
        }

        // Set up MaterialToolbar
        val toolbar = findViewById<MaterialToolbar?>(R.id.toolbar)
        toolbar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Register incoming call broadcast receiver
        incomingCallReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("MainActivity", getString(R.string.log_received_broadcast, intent?.action))
                if (intent?.action == getString(R.string.intent_action_incoming_call)) {
                    val phoneNumber = intent.getStringExtra(getString(R.string.intent_extra_phone_number))
                    Log.d("MainActivity", getString(R.string.log_processing_incoming_call, phoneNumber))
                    if (!phoneNumber.isNullOrEmpty()) {
                        callViewModel.onIncomingCall(phoneNumber)
                    }
                }
            }
        }
        val incomingCallFilter = IntentFilter(getString(R.string.intent_action_incoming_call))
        registerReceiver(incomingCallReceiver, incomingCallFilter)

        // Observe unknown caller LiveData
        callViewModel.unknownCaller.observe(this) { callerInfo ->
            if (callerInfo != null) {
                showUnknownCallerNotification(callerInfo)
            }
        }

        // Request permissions
        requestPermissions()
        createNotificationChannel()
        
        // Start call monitoring service
        startCallMonitoringService()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the incoming call receiver
        unregisterReceiver(incomingCallReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 0) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                Log.d("MainActivity", getString(R.string.log_permission_result, permission, granted))
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS, // For Android 13+
            Manifest.permission.READ_PHONE_NUMBERS, // For Android 8+
            Manifest.permission.ANSWER_PHONE_CALLS, // For Android 10+
            Manifest.permission.MODIFY_PHONE_STATE, // For call state modification
            Manifest.permission.FOREGROUND_SERVICE, // For Android 9+
            Manifest.permission.READ_CALL_LOG // For call log fallback
        )
        
        // Check current permission status
        permissions.forEach { permission ->
            val granted = ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            Log.d("MainActivity", getString(R.string.log_permission_granted, permission, granted))
        }
        
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_name),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = getString(R.string.notification_channel_description)
            channel.enableVibration(true)
            channel.enableLights(true)
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("MainActivity", getString(R.string.log_notification_channel_created))
        } else {
            Log.d("MainActivity", getString(R.string.log_notification_channel_not_needed))
        }
    }

    private fun showUnknownCallerNotification(callerInfo: CallerInfo) {
        Log.d("MainActivity", getString(R.string.log_showing_notification, callerInfo.phoneNumber))
        
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                Log.e("MainActivity", getString(R.string.log_notification_permission_denied))
                return
            }
        }
        
        val intent = Intent(this, NotifyCallerService::class.java).apply {
            putExtra(getString(R.string.intent_extra_phone_number), callerInfo.phoneNumber)
        }
        Log.d("MainActivity", "Created service intent: $intent")
        Log.d("MainActivity", "Intent extras: ${intent.extras}")
        
        val pendingIntent = PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("MainActivity", "Created pending intent: $pendingIntent")
        
        val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, getString(R.string.notification_action), pendingIntent)
            .build()
        
        try {
            NotificationManagerCompat.from(this).notify(1001, notification)
            Log.d("MainActivity", getString(R.string.log_notification_sent))
        } catch (e: Exception) {
            Log.e("MainActivity", getString(R.string.log_notification_failed), e)
        }
    }
    
    private fun startCallMonitoringService() {
        // For Android 8+ we might need a foreground service for reliable call detection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("MainActivity", getString(R.string.log_starting_monitoring_service))
            // Start the foreground service for more reliable call detection
            val serviceIntent = Intent(this, CallMonitoringService::class.java)
            startForegroundService(serviceIntent)
        }
        
        Log.d("MainActivity", getString(R.string.log_call_monitoring_initialized))
    }
    
    private fun checkSmsCapability() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val smsCapable = telephonyManager.simState == TelephonyManager.SIM_STATE_READY
        val smsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        
        Log.d("MainActivity", "SMS Capability Check:")
        Log.d("MainActivity", "  Device SMS capable: $smsCapable")
        Log.d("MainActivity", "  SIM state: ${telephonyManager.simState}")
        Log.d("MainActivity", "  SMS permission granted: ${smsPermission == PackageManager.PERMISSION_GRANTED}")
        
        val message = "SMS Check: Capable=$smsCapable, SIM=${telephonyManager.simState}, Permission=${smsPermission == PackageManager.PERMISSION_GRANTED}"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun importAllUnknownCallLogs() {
        Log.d("MainActivity", "Starting import of all unknown call logs")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "Checking call log permission")
                val callLogPermission = ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CALL_LOG)
                Log.d("MainActivity", "Call log permission granted: ${callLogPermission == PackageManager.PERMISSION_GRANTED}")
                
                if (callLogPermission != PackageManager.PERMISSION_GRANTED) {
                    Log.e("MainActivity", "Call log permission not granted")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Call log permission required", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                Log.d("MainActivity", "Calling ViewModel import method")
                val importedCount = callViewModel.importAllUnknownCallLogs()
                Log.d("MainActivity", "Import completed. Imported $importedCount unknown call logs")
                
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Imported $importedCount unknown call logs", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error importing call logs", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error importing call logs: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
} 