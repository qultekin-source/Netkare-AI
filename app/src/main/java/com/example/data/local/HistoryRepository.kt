package com.example.data.local

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: EnhancementDao) {
    val allRecords: Flow<List<EnhancementRecord>> = dao.getAllRecords()

    suspend fun addRecord(record: EnhancementRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
