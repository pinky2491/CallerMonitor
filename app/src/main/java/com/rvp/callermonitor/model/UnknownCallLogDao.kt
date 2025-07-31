package com.rvp.callermonitor.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for unknown call logs.
 */
@Dao
interface UnknownCallLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UnknownCallLog)

    @Query("SELECT * FROM unknown_call_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UnknownCallLog>>

    @Query("DELETE FROM unknown_call_logs WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM unknown_call_logs")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM unknown_call_logs WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getByNumber(phoneNumber: String): UnknownCallLog?
} 