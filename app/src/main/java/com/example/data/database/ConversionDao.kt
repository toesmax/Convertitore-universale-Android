package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ConversionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ConversionRecord): Long

    @Query("DELETE FROM conversion_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM conversion_records")
    suspend fun clearAllRecords()
}
