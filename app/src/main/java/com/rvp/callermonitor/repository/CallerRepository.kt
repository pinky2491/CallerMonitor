package com.rvp.callermonitor.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import com.rvp.callermonitor.model.UnknownCallLog
import com.rvp.callermonitor.model.UnknownCallLogDao
import com.rvp.callermonitor.model.BlockedNumber
import com.rvp.callermonitor.model.BlockedNumberDao
import com.rvp.callermonitor.model.ReportedSpamNumber
import com.rvp.callermonitor.model.ReportedSpamNumberDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import android.provider.CallLog
import android.util.Log

/**
 * Repository for contact-related operations.
 */
@Singleton
class CallerRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val unknownCallLogDao: UnknownCallLogDao,
    private val blockedNumberDao: BlockedNumberDao,
    private val reportedSpamNumberDao: ReportedSpamNumberDao,
    @ApplicationContext private val context: Context
) {
    /**
     * Checks if the given phone number is in the user's contacts.
     */
    fun isNumberInContacts(phoneNumber: String): Boolean {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        val exists = cursor?.moveToFirst() == true
        cursor?.close()
        return exists
    }

    /**
     * Insert a log for an unknown call.
     */
    suspend fun insertUnknownCallLog(log: UnknownCallLog) {
        unknownCallLogDao.insert(log)
    }

    /**
     * Get all unknown call logs as a Flow.
     */
    fun getUnknownCallLogs(): Flow<List<UnknownCallLog>> = unknownCallLogDao.getAllLogs()

    /**
     * Delete an unknown call log by id.
     */
    suspend fun deleteUnknownCallLog(id: Int) = unknownCallLogDao.deleteById(id)

    /**
     * Delete all unknown call logs.
     */
    suspend fun deleteAllUnknownCallLogs() = unknownCallLogDao.deleteAll()

    suspend fun blockNumberLocally(phoneNumber: String) {
        blockedNumberDao.insert(BlockedNumber(phoneNumber))
    }
    suspend fun isNumberBlockedLocally(phoneNumber: String): Boolean = blockedNumberDao.isBlocked(phoneNumber)
    suspend fun getAllBlockedNumbers(): List<BlockedNumber> = blockedNumberDao.getAll()
    suspend fun unblockNumberLocally(phoneNumber: String) {
        blockedNumberDao.delete(BlockedNumber(phoneNumber))
    }

    suspend fun isNumberSpam(phoneNumber: String): Boolean {
        return isNumberInStaticSpamList(phoneNumber) || reportedSpamNumberDao.isSpam(phoneNumber)
    }
    private fun isNumberInStaticSpamList(phoneNumber: String): Boolean {
        return try {
            val input = context.assets.open("spam_numbers.json")
            val json = input.bufferedReader().use { it.readText() }
            val list = com.google.gson.Gson().fromJson(json, Array<String>::class.java).toList()
            list.contains(phoneNumber)
        } catch (e: Exception) {
            false
        }
    }
    suspend fun reportNumberAsSpam(phoneNumber: String) {
        reportedSpamNumberDao.insert(ReportedSpamNumber(phoneNumber))
    }
    suspend fun getAllReportedSpamNumbers(): List<ReportedSpamNumber> = reportedSpamNumberDao.getAll()
    
    /**
     * Import all unknown call logs from device call history
     */
    suspend fun importAllUnknownCallLogs(): Int {
        Log.d("CallerRepository", "Starting import of all unknown call logs from device")
        
        val callLogs = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )
        
        Log.d("CallerRepository", "Call log query result: ${callLogs?.count ?: 0} total calls")
        
        var importedCount = 0
        var totalCalls = 0
        var incomingCalls = 0
        var unknownNumbers = 0
        val processedNumbers = mutableSetOf<String>()
        
        try {
            if (callLogs != null) {
                while (callLogs.moveToNext()) {
                    totalCalls++
                    val number = callLogs.getString(callLogs.getColumnIndex(CallLog.Calls.NUMBER))
                    val type = callLogs.getInt(callLogs.getColumnIndex(CallLog.Calls.TYPE))
                    val date = callLogs.getLong(callLogs.getColumnIndex(CallLog.Calls.DATE))
                    
                    Log.d("CallerRepository", "Processing call $totalCalls: number=$number, type=$type, date=$date")
                    
                    // Only process incoming calls (type = 1)
                    if (number != null && type == CallLog.Calls.INCOMING_TYPE) {
                        incomingCalls++
                        Log.d("CallerRepository", "Found incoming call: $number")
                        
                        // Clean the number (remove spaces, dashes, etc.)
                        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                        
                        // Skip if already processed or empty
                        if (cleanNumber.isNotEmpty() && !processedNumbers.contains(cleanNumber)) {
                            Log.d("CallerRepository", "Checking if number is in contacts: $cleanNumber")
                            
                            // Check if number is in contacts
                            if (!isNumberInContacts(cleanNumber)) {
                                unknownNumbers++
                                Log.d("CallerRepository", "Number not in contacts: $cleanNumber")
                                
                                // Check if already exists in our database
                                val existingLog = unknownCallLogDao.getByNumber(cleanNumber)
                                if (existingLog == null) {
                                    val log = UnknownCallLog(
                                        phoneNumber = cleanNumber,
                                        timestamp = date
                                    )
                                    insertUnknownCallLog(log)
                                    processedNumbers.add(cleanNumber)
                                    importedCount++
                                    Log.d("CallerRepository", "Imported unknown call log: $cleanNumber")
                                } else {
                                    Log.d("CallerRepository", "Number already exists in database: $cleanNumber")
                                }
                            } else {
                                Log.d("CallerRepository", "Number is in contacts: $cleanNumber")
                            }
                        } else {
                            Log.d("CallerRepository", "Skipping number (empty or duplicate): $cleanNumber")
                        }
                    }
                }
            }
        } finally {
            callLogs?.close()
        }
        
        Log.d("CallerRepository", "Import summary:")
        Log.d("CallerRepository", "  Total calls processed: $totalCalls")
        Log.d("CallerRepository", "  Incoming calls: $incomingCalls")
        Log.d("CallerRepository", "  Unknown numbers: $unknownNumbers")
        Log.d("CallerRepository", "  Imported: $importedCount")
        Log.d("CallerRepository", "Import completed. Imported $importedCount unknown call logs")
        return importedCount
    }
} 