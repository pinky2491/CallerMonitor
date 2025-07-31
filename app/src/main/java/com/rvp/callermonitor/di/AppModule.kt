package com.rvp.callermonitor.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.room.Room
import com.rvp.callermonitor.model.CallerMonitorDatabase
import com.rvp.callermonitor.model.UnknownCallLogDao
import com.rvp.callermonitor.model.BlockedNumberDao
import com.rvp.callermonitor.model.ReportedSpamNumberDao
import dagger.hilt.android.qualifiers.ApplicationContext
import com.rvp.callermonitor.R

/**
 * Hilt module for app-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext app: Context): ContentResolver =
        app.contentResolver

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext app: Context): CallerMonitorDatabase =
        Room.databaseBuilder(app, CallerMonitorDatabase::class.java, app.getString(R.string.database_name)).build()

    @Provides
    fun provideUnknownCallLogDao(db: CallerMonitorDatabase): UnknownCallLogDao =
        db.unknownCallLogDao()

    @Provides
    fun provideBlockedNumberDao(db: CallerMonitorDatabase): BlockedNumberDao =
        db.blockedNumberDao()

    @Provides
    fun provideReportedSpamNumberDao(db: CallerMonitorDatabase): ReportedSpamNumberDao =
        db.reportedSpamNumberDao()
} 