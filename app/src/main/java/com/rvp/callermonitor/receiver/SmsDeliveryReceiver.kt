package com.rvp.callermonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import android.app.Activity

class SmsDeliveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "SMS_SENT" -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d("SmsDeliveryReceiver", "SMS sent successfully")
                        Toast.makeText(context, "SMS sent successfully", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Log.e("SmsDeliveryReceiver", "SMS failed: Generic failure")
                        Toast.makeText(context, "SMS failed: Generic failure", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Log.e("SmsDeliveryReceiver", "SMS failed: No service")
                        Toast.makeText(context, "SMS failed: No service", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Log.e("SmsDeliveryReceiver", "SMS failed: Null PDU")
                        Toast.makeText(context, "SMS failed: Null PDU", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Log.e("SmsDeliveryReceiver", "SMS failed: Radio off")
                        Toast.makeText(context, "SMS failed: Radio off", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.e("SmsDeliveryReceiver", "SMS failed: Unknown error code $resultCode")
                        Toast.makeText(context, "SMS failed: Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "SMS_DELIVERED" -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d("SmsDeliveryReceiver", "SMS delivered successfully")
                        Toast.makeText(context, "SMS delivered successfully", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.e("SmsDeliveryReceiver", "SMS delivery failed: $resultCode")
                        Toast.makeText(context, "SMS delivery failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
} 