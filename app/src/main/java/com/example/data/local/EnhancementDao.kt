package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnhancementDao {
    @Query("SELECT * FROM enhancement_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<EnhancementRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EnhancementRecord): Long

    @Query("DELETE FROM enhancement_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM enhancement_records")
    suspend fun clearAll()
}
