package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao,
    private val studySessionDao: StudySessionDao,
    private val distractionFilterDao: DistractionFilterDao
) {
    // Folders
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    
    suspend fun getFolderById(id: Int): Folder? = folderDao.getFolderById(id)
    
    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    
    suspend fun deleteFolder(folder: Folder) {
        // Safe dissociation of notes before folder removal
        noteDao.removeNotesFromFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    // Notes/Lectures
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    
    fun getNotesInFolder(folderId: Int): Flow<List<Note>> = noteDao.getNotesInFolder(folderId)
    
    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)
    
    val pinnedNotes: Flow<List<Note>> = noteDao.getPinnedNotes()
    
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    // Study Sessions (Focus Tracking)
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()
    
    suspend fun insertSession(session: StudySession): Long = studySessionDao.insertSession(session)

    // Blocked Filters
    val allFilters: Flow<List<DistractionFilter>> = distractionFilterDao.getAllFilters()
    
    suspend fun getActiveFilters(): List<DistractionFilter> = distractionFilterDao.getActiveFilters()
    
    suspend fun insertFilter(filter: DistractionFilter): Long = distractionFilterDao.insertFilter(filter)
    
    suspend fun updateFilter(filter: DistractionFilter) = distractionFilterDao.updateFilter(filter)
    
    suspend fun deleteFilter(filter: DistractionFilter) = distractionFilterDao.deleteFilter(filter)
    
    suspend fun incrementBlockCount(id: Int) = distractionFilterDao.incrementBlockCount(id)
    
    suspend fun incrementBlockCountByTarget(target: String) = distractionFilterDao.incrementBlockCountByTarget(target)
}
