package com.rvp.callermonitor.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reported_spam_numbers")
data class ReportedSpamNumber(
    @PrimaryKey val phoneNumber: String
) 