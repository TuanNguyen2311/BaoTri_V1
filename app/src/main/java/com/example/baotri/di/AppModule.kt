package com.example.baotri.di

import android.content.Context
import com.example.baotri.data.db.AppDatabase
import com.example.baotri.data.db.dao.*
import com.example.baotri.data.repository.*
import com.example.baotri.domain.repository.*
import com.example.baotri.util.SessionManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        AppDatabase.getInstance(ctx)

    @Provides @Singleton fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides @Singleton fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()
    @Provides @Singleton fun provideLogDao(db: AppDatabase): MaintenanceLogDao = db.maintenanceLogDao()
    @Provides @Singleton fun provideBackupHistoryDao(db: AppDatabase): BackupHistoryDao = db.backupHistoryDao()
    @Provides @Singleton fun provideLogStatusHistoryDao(db: AppDatabase): LogStatusHistoryDao = db.logStatusHistoryDao()

    @Provides @Singleton fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideSessionRepository(sessionManager: SessionManager): SessionRepository =
        SessionRepositoryImpl(sessionManager)

    @Provides @Singleton
    fun provideUserRepository(dao: UserDao): UserRepository =
        UserRepositoryImpl(dao)

    @Provides @Singleton
    fun provideDeviceRepository(deviceDao: DeviceDao): DeviceRepository =
        DeviceRepositoryImpl(deviceDao)

    @Provides @Singleton
    fun provideLogRepository(
        logDao: MaintenanceLogDao,
        deviceDao: DeviceDao,
        historyDao: LogStatusHistoryDao,
        gson: Gson
    ): MaintenanceLogRepository =
        MaintenanceLogRepositoryImpl(logDao, deviceDao, historyDao, gson)

    @Provides @Singleton
    fun provideBackupRepository(
        db: AppDatabase,
        historyDao: BackupHistoryDao,
        gson: Gson,
        @ApplicationContext ctx: Context
    ): BackupRepository =
        BackupRepositoryImpl(db, historyDao, gson, ctx)
}
