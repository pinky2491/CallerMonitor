package com.rvp.callermonitor.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rvp.callermonitor.model.BlockedNumber
import com.rvp.callermonitor.model.BlockedNumberDao
import com.rvp.callermonitor.model.ReportedSpamNumber
import com.rvp.callermonitor.model.ReportedSpamNumberDao

/**
 * Room database for CallerMonitor app.
 */
@Database(entities = [UnknownCallLog::class, BlockedNumber::class, ReportedSpamNumber::class], version = 3, exportSchema = false)
abstract class CallerMonitorDatabase : RoomDatabase() {
    abstract fun unknownCallLogDao(): UnknownCallLogDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun reportedSpamNumberDao(): ReportedSpamNumberDao
} 