package com.rvp.callermonitor.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface ReportedSpamNumberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reportedSpamNumber: ReportedSpamNumber)

    @Query("SELECT * FROM reported_spam_numbers")
    suspend fun getAll(): List<ReportedSpamNumber>

    @Query("SELECT EXISTS(SELECT 1 FROM reported_spam_numbers WHERE phoneNumber = :phoneNumber)")
    suspend fun isSpam(phoneNumber: String): Boolean

    @Delete
    suspend fun delete(reportedSpamNumber: ReportedSpamNumber)
} 