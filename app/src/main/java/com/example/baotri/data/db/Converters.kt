package com.example.baotri.data.db

import androidx.room.TypeConverter
import com.example.baotri.data.model.*

class Converters {

    @TypeConverter fun fromUserRole(v: UserRole): String = v.name
    @TypeConverter fun toUserRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun fromLogType(v: LogType): String = v.name
    @TypeConverter fun toLogType(v: String): LogType = LogType.valueOf(v)

    @TypeConverter fun fromLogStatus(v: LogStatus): String = v.name
    @TypeConverter fun toLogStatus(v: String): LogStatus = LogStatus.valueOf(v)

    @TypeConverter fun fromBackupAction(v: BackupAction): String = v.name
    @TypeConverter fun toBackupAction(v: String): BackupAction = BackupAction.valueOf(v)
}
