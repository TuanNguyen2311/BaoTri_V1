package com.example.baotri.data.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.baotri.data.db.dao.*
import com.example.baotri.data.model.*
import com.example.baotri.util.SecurityUtil

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        MaintenanceLogEntity::class,
        BackupHistoryEntity::class,
        LogStatusHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun backupHistoryDao(): BackupHistoryDao
    abstract fun logStatusHistoryDao(): LogStatusHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "baotri_db"
                )
                .addCallback(SeedCallback())
                .build()
                .also { INSTANCE = it }
            }
        }
    }

    // Seed default admin account on first install
    private class SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert default admin: username=admin, password=1234
            val passwordHash = SecurityUtil.sha256("1234")
            db.execSQL("""
                INSERT INTO users (username, passwordHash, pinHash, role, fullName, isActive, isFirstLogin, createdAt)
                VALUES ('admin', '$passwordHash', NULL, 'MANAGER', 'Quản Lý', 1, 1, ${System.currentTimeMillis()})
            """)
        }
    }
}
