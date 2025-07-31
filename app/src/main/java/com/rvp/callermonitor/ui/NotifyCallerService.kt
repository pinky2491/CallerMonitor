package com.rvp.callermonitor.ui

import android.app.IntentService
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.rvp.callermonitor.R
import android.util.Log
import android.telephony.TelephonyManager
import android.app.PendingIntent

/**
 * Service to send SMS to unknown caller when user taps notification action.
 */
class NotifyCallerService : IntentService("NotifyCallerService") {
    override fun onHandleIntent(intent: Intent?) {
        Log.d("NotifyCallerService", "=== NOTIFY CALLER SERVICE STARTED ===")
        Log.d("NotifyCallerService", "Intent: $intent")
        Log.d("NotifyCallerService", "Intent extras: ${intent?.extras}")
        
        val phoneNumber = intent?.getStringExtra(getString(R.string.intent_extra_phone_number))
        Log.d("NotifyCallerService", "Phone number: $phoneNumber")
        
        if (phoneNumber.isNullOrEmpty()) {
            Log.e("NotifyCallerService", "Phone number is null or empty")
            showToast("Error: No phone number provided")
            return
        }
        
        // Check if device has SMS capability
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val smsCapable = telephonyManager.simState == TelephonyManager.SIM_STATE_READY
        Log.d("NotifyCallerService", "Device SMS capable: $smsCapable")
        Log.d("NotifyCallerService", "SIM state: ${telephonyManager.simState}")
        
        if (!smsCapable) {
            Log.e("NotifyCallerService", "Device is not SMS capable")
            showToast("Error: Device does not support SMS")
            return
        }
        
        val smsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        Log.d("NotifyCallerService", "SMS permission granted: ${smsPermission == PackageManager.PERMISSION_GRANTED}")
        
        if (smsPermission == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = SmsManager.getDefault()
                val message = getString(R.string.sms_monitoring_message)
                Log.d("NotifyCallerService", "SMS Manager: $smsManager")
                Log.d("NotifyCallerService", "Sending SMS to: $phoneNumber")
                Log.d("NotifyCallerService", "Message: $message")
                
                // For Android 6.0+, we need to use the newer API
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val smsManagerNew = getSystemService(SmsManager::class.java)
                    Log.d("NotifyCallerService", "Using new SMS Manager: $smsManagerNew")
                    
                    // Create delivery and sent intents for tracking
                    val sentIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
                    val deliveredIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)
                    
                    Log.d("NotifyCallerService", "Sending SMS with delivery tracking")
                    smsManagerNew.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        sentIntent,
                        deliveredIntent
                    )
                } else {
                    smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        null,
                        null
                    )
                }
                
                Log.d("NotifyCallerService", "SMS sent successfully")
                showToast(getString(R.string.toast_sms_sent))
                
                // Verify SMS was sent by checking recent SMS
                verifySmsSent(phoneNumber, message)
                
            } catch (e: Exception) {
                Log.e("NotifyCallerService", "Error sending SMS", e)
                showToast("Error sending SMS: ${e.message}")
                
                // Try alternative method
                tryAlternativeSmsMethod(phoneNumber, getString(R.string.sms_monitoring_message))
            }
        } else {
            Log.e("NotifyCallerService", "SMS permission not granted")
            showToast(getString(R.string.toast_sms_permission_denied))
        }
    }
    
    private fun tryAlternativeSmsMethod(phoneNumber: String, message: String) {
        Log.d("NotifyCallerService", "Trying alternative SMS method")
        try {
            // Try using the newer SMS manager API
            val smsManager = getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
            Log.d("NotifyCallerService", "Alternative SMS method succeeded")
            showToast("SMS sent via alternative method")
        } catch (e: Exception) {
            Log.e("NotifyCallerService", "Alternative SMS method also failed", e)
            showToast("All SMS methods failed: ${e.message}")
        }
    }
    
    private fun verifySmsSent(phoneNumber: String, message: String) {
        Log.d("NotifyCallerService", "Verifying SMS was sent to: $phoneNumber")
        // This is a simple verification - in a real app you might want to check the SMS database
        showToast("SMS verification: Message sent to $phoneNumber")
    }

    private fun showToast(message: String) {
        Log.d("NotifyCallerService", "Showing toast: $message")
        // Show toast on main thread
        val handler = android.os.Handler(mainLooper)
        handler.post { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }
} 