package com.example.baotri.data.repository

import com.example.baotri.data.db.dao.DeviceDao
import com.example.baotri.data.model.LogStatus
import com.example.baotri.domain.model.Device
import com.example.baotri.domain.model.DeviceStats
import com.example.baotri.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override fun getAllDevices(): Flow<List<Device>> =
        deviceDao.getAllWithLatestStatus().map { list ->
            list.map { row ->
                val status = row.latestStatus
                    ?.let { runCatching { LogStatus.valueOf(it) }.getOrNull() }
                    ?.toDomain()
                row.device.toDomain(latestStatus = status)
            }
        }

    override fun searchDevices(query: String): Flow<List<Device>> =
        deviceDao.search(query).map { list -> list.map { it.toDomain() } }

    override fun filterByArea(area: String): Flow<List<Device>> =
        deviceDao.filterByArea(area).map { list -> list.map { it.toDomain() } }

    override suspend fun getDeviceById(id: Long): Device? =
        deviceDao.findById(id)?.toDomain()

    override suspend fun getDeviceByCode(code: String): Device? =
        deviceDao.findByCode(code)?.toDomain()

    override suspend fun addDevice(device: Device): Long =
        deviceDao.insert(device.toEntity())

    override suspend fun updateDevice(device: Device) =
        deviceDao.update(device.toEntity())

    override suspend fun deleteDevice(deviceId: Long) {
        val entity = deviceDao.findById(deviceId) ?: return
        deviceDao.delete(entity)
    }

    override suspend fun updateQrPath(deviceId: Long, path: String) =
        deviceDao.updateQrPath(deviceId, path)

    override suspend fun getStats(): DeviceStats = DeviceStats(
        total           = deviceDao.count(),
        pending         = deviceDao.countWithPendingIssues(),
        expiredWarranty = deviceDao.countExpiredWarranty()
    )
}