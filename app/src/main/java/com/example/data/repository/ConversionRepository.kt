package com.example.data.repository

import com.example.data.database.ConversionDao
import com.example.data.database.ConversionRecord
import kotlinx.coroutines.flow.Flow

class ConversionRepository(private val conversionDao: ConversionDao) {
    val allRecords: Flow<List<ConversionRecord>> = conversionDao.getAllRecords()

    suspend fun insert(record: ConversionRecord): Long {
        return conversionDao.insertRecord(record)
    }

    suspend fun deleteById(id: Int) {
        conversionDao.deleteRecordById(id)
    }

    suspend fun clearAll() {
        conversionDao.clearAllRecords()
    }
}
