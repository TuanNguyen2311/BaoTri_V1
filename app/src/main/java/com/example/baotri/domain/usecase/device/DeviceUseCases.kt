package com.example.baotri.domain.usecase.device

import com.example.baotri.domain.model.Device
import com.example.baotri.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeviceByIdUseCase @Inject constructor(private val repo: DeviceRepository) {
    suspend operator fun invoke(id: Long): Device? = repo.getDeviceById(id)
}

class GetAllDevicesUseCase @Inject constructor(private val repo: DeviceRepository) {
    operator fun invoke(): Flow<List<Device>> = repo.getAllDevices()
}

class SearchDevicesUseCase @Inject constructor(private val repo: DeviceRepository) {
    operator fun invoke(query: String): Flow<List<Device>> =
        if (query.isBlank()) repo.getAllDevices() else repo.searchDevices(query)
}

class GetDeviceByCodeUseCase @Inject constructor(private val repo: DeviceRepository) {
    suspend operator fun invoke(code: String): Result<Device> {
        val device = repo.getDeviceByCode(code.trim().uppercase())
            ?: return Result.failure(Exception("Không tìm thấy thiết bị với mã: $code"))
        return Result.success(device)
    }
}

class SaveDeviceUseCase @Inject constructor(private val repo: DeviceRepository) {
    suspend operator fun invoke(device: Device): Result<Long> {
        if (device.code.isBlank()) return Result.failure(Exception("Mã thiết bị không được để trống"))
        if (device.name.isBlank()) return Result.failure(Exception("Tên thiết bị không được để trống"))
        if (device.category.isBlank()) return Result.failure(Exception("Danh mục không được để trống"))
        if (device.location.isBlank()) return Result.failure(Exception("Vị trí không được để trống"))

        return try {
            if (device.id == 0L) {
                // Check duplicate code
                val existing = repo.getDeviceByCode(device.code)
                if (existing != null)
                    return Result.failure(Exception("Mã thiết bị '${device.code}' đã tồn tại"))
                val id = repo.addDevice(device)
                Result.success(id)
            } else {
                repo.updateDevice(device)
                Result.success(device.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteDeviceUseCase @Inject constructor(private val repo: DeviceRepository) {
    suspend operator fun invoke(deviceId: Long): Result<Unit> {
        return try {
            repo.deleteDevice(deviceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
