package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Int): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, lastModifiedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, lastModifiedAt DESC")
    fun getNotesInFolder(folderId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY lastModifiedAt DESC")
    fun getPinnedNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)
    
    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun removeNotesFromFolder(folderId: Int)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long
}

@Dao
interface DistractionFilterDao {
    @Query("SELECT * FROM distraction_filters ORDER BY type ASC, name ASC")
    fun getAllFilters(): Flow<List<DistractionFilter>>

    @Query("SELECT * FROM distraction_filters WHERE isEnabled = 1")
    suspend fun getActiveFilters(): List<DistractionFilter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: DistractionFilter): Long

    @Update
    suspend fun updateFilter(filter: DistractionFilter)

    @Delete
    suspend fun deleteFilter(filter: DistractionFilter)

    @Query("UPDATE distraction_filters SET blockCount = blockCount + 1 WHERE id = :id")
    suspend fun incrementBlockCount(id: Int)
    
    @Query("UPDATE distraction_filters SET blockCount = blockCount + 1 WHERE target = :target")
    suspend fun incrementBlockCountByTarget(target: String)
}
