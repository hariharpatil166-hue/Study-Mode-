package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#4F46E5", // Indigo secondary
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int? = null, // None/uncategorized if null
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int? = null, // Linked note/lecture for context
    val noteTitle: String? = null, // Redundant cached note title
    val durationMinutes: Int,
    val startTime: Long = System.currentTimeMillis(),
    val completed: Boolean = false
) : Serializable

@Entity(tableName = "distraction_filters")
data class DistractionFilter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "app" or "website"
    val name: String, // "Instagram", "Facebook", etc.
    val target: String, // Package name or domain URL
    val isEnabled: Boolean = true,
    val blockCount: Int = 0
) : Serializable
