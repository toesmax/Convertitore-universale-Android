package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_records")
data class ConversionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val sourceExtension: String,
    val destExtension: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val outputFilePath: String? = null
)
