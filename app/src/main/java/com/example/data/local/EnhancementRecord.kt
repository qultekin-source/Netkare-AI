package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enhancement_records")
data class EnhancementRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaType: String, // "VIDEO" or "PHOTO"
    val title: String,
    val presetName: String,
    val sourceResolution: String,
    val targetResolution: String,
    val outputPath: String,
    val aiSummary: String,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
