package com.rvp.callermonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.rvp.callermonitor.R
import android.database.Cursor
import android.provider.CallLog
import kotlinx.coroutines.*

/**
 * BroadcastReceiver to listen for incoming calls.
 */
class CallReceiver : BroadcastReceiver() {
    private var lastCallTime: Long = 0
    private var isRinging = false
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("CallReceiver", "=== CALL RECEIVER TRIGGERED ===")
        Log.d("CallReceiver", context.getString(R.string.log_received_intent, intent.action))
        Log.d("CallReceiver", context.getString(R.string.log_intent_extras, intent.extras))
        
        // Handle test broadcast from the app
        if (intent.action == context.getString(R.string.intent_action_test_call)) {
            val testNumber = intent.getStringExtra(context.getString(R.string.intent_extra_phone_number)) ?: context.getString(R.string.test_phone_number_2)
            Log.d("CallReceiver", context.getString(R.string.log_received_test_call, testNumber))
            
            // Send a broadcast to MainActivity to handle the incoming call
            val broadcastIntent = Intent(context.getString(R.string.intent_action_incoming_call))
            broadcastIntent.putExtra(context.getString(R.string.intent_extra_phone_number), testNumber)
            context.sendBroadcast(broadcastIntent)
            Log.d("CallReceiver", context.getString(R.string.log_sent_broadcast_test, testNumber))
            return
        }
        
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("CallReceiver", context.getString(R.string.log_phone_state, state))
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    isRinging = true
                    lastCallTime = System.currentTimeMillis()
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    Log.d("CallReceiver", context.getString(R.string.log_incoming_number, incomingNumber))
                    
                    if (!incomingNumber.isNullOrEmpty()) {
                        // Send a broadcast to MainActivity to handle the incoming call
                        val broadcastIntent = Intent(context.getString(R.string.intent_action_incoming_call))
                        broadcastIntent.putExtra(context.getString(R.string.intent_extra_phone_number), incomingNumber)
                        context.sendBroadcast(broadcastIntent)
                        Log.d("CallReceiver", context.getString(R.string.log_sent_broadcast_incoming, incomingNumber))
                    } else {
                        Log.w("CallReceiver", context.getString(R.string.log_incoming_number_null))
                        // Try to get the number from call log after a short delay
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(2000) // Wait 2 seconds for call log to be updated
                            if (isRinging) {
                                val phoneNumber = getLatestIncomingCall(context)
                                if (!phoneNumber.isNullOrEmpty()) {
                                    Log.d("CallReceiver", "Got number from call log: $phoneNumber")
                                    val broadcastIntent = Intent(context.getString(R.string.intent_action_incoming_call))
                                    broadcastIntent.putExtra(context.getString(R.string.intent_extra_phone_number), phoneNumber)
                                    context.sendBroadcast(broadcastIntent)
                                }
                            }
                        }
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    isRinging = false
                    Log.d("CallReceiver", context.getString(R.string.log_phone_state_not_ringing, state))
                }
                else -> {
                    Log.d("CallReceiver", context.getString(R.string.log_phone_state_not_ringing, state))
                }
            }
        } else {
            Log.d("CallReceiver", context.getString(R.string.log_intent_action_not_phone_state, intent.action))
        }
    }
    
    private fun getLatestIncomingCall(context: Context): String? {
        return try {
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.INCOMING_TYPE.toString(), (lastCallTime - 10000).toString()),
                "${CallLog.Calls.DATE} DESC"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                    Log.d("CallReceiver", "Found number in call log: $number")
                    number
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error reading call log", e)
            null
        }
    }
} 