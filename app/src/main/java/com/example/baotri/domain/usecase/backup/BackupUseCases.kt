package com.example.baotri.domain.usecase.backup

import com.example.baotri.domain.model.BackupHistory
import com.example.baotri.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(): Result<ByteArray> = try {
        Result.success(repo.exportBackup())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class ImportBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(data: ByteArray): Result<Unit> = try {
        repo.importBackup(data)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class GetBackupHistoryUseCase @Inject constructor(private val repo: BackupRepository) {
    operator fun invoke(): Flow<List<BackupHistory>> = repo.getBackupHistory()
}

class GetLatestBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(): BackupHistory? = repo.getLatestBackup()
}

class RecordBackupUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(
        action: String,
        fileName: String,
        sizeBytes: Long,
        destination: String
    ) = repo.recordBackup(action, fileName, sizeBytes, destination)
}

class PrepareBackupForSharingUseCase @Inject constructor(private val repo: BackupRepository) {
    suspend operator fun invoke(data: ByteArray, fileName: String): String =
        repo.prepareForSharing(data, fileName)
}
