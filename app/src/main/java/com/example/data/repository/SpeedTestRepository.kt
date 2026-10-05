package com.example.data.repository

import com.example.core.speedtest.SpeedTestResult
import com.example.data.local.dao.SpeedTestDao
import com.example.data.local.entities.toDomainModel
import com.example.data.local.entities.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SpeedTestRepository {
    fun getAllSpeedTests(): Flow<List<SpeedTestResult>>
    suspend fun saveSpeedTest(result: SpeedTestResult): Long
    suspend fun deleteSpeedTest(id: Long)
    suspend fun clearAll()
}

class DefaultSpeedTestRepository(
    private val speedTestDao: SpeedTestDao
) : SpeedTestRepository {

    override fun getAllSpeedTests(): Flow<List<SpeedTestResult>> {
        return speedTestDao.getAllSpeedTests().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun saveSpeedTest(result: SpeedTestResult): Long {
        return speedTestDao.insertSpeedTest(result.toEntity())
    }

    override suspend fun deleteSpeedTest(id: Long) {
        speedTestDao.deleteSpeedTestById(id)
    }

    override suspend fun clearAll() {
        speedTestDao.clearAllSpeedTests()
    }
}
