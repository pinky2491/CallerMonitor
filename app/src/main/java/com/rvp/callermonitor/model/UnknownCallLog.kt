package com.rvp.callermonitor.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a log of an unknown call.
 */
@Entity(tableName = "unknown_call_logs")
data class UnknownCallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long
) 