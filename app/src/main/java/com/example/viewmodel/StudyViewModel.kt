package com.example.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DistractionFilter
import com.example.data.Folder
import com.example.data.Note
import com.example.data.StudyRepository
import com.example.data.StudySession
import com.example.services.FocusNotificationService
import com.example.services.StudySessionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudyViewModel(private val repository: StudyRepository) : ViewModel() {

    // Databases flows
    val folders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyHistory: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distractionFilters: StateFlow<List<DistractionFilter>> = repository.allFilters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Search & Filter States
    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered Notes
    val filteredNotes: StateFlow<List<Note>> = combine(notes, _selectedFolderId, _searchQuery) { allNotes, folderId, query ->
        allNotes.filter { note ->
            val matchFolder = (folderId == null || note.folderId == folderId)
            val matchQuery = query.isEmpty() || 
                    note.title.contains(query, ignoreCase = true) || 
                    note.content.contains(query, ignoreCase = true)
            matchFolder && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Study Timer States (synchronized with Foreground Service)
    private val _isStudyModeActive = MutableStateFlow(false)
    val isStudyModeActive = _isStudyModeActive.asStateFlow()

    private val _studyTimeRemaining = MutableStateFlow(0L)
    val studyTimeRemaining = _studyTimeRemaining.asStateFlow()

    private val _studyTimeTotal = MutableStateFlow(1500L) // Default 25min
    val studyTimeTotal = _studyTimeTotal.asStateFlow()

    private val _linkedNote = MutableStateFlow<Note?>(null)
    val linkedNote = _linkedNote.asStateFlow()

    // Flag representing standard blocked app popup overlay
    private val _blockedAppTrigger = MutableStateFlow<String?>(null)
    val blockedAppTrigger = _blockedAppTrigger.asStateFlow()

    // Simulated browser search bar & filter portal for distracting websites
    private val _activeWebViewUrl = MutableStateFlow<String?>(null)
    val activeWebViewUrl = _activeWebViewUrl.asStateFlow()

    private val _isDistractingWebBlocked = MutableStateFlow(false)
    val isDistractingWebBlocked = _isDistractingWebBlocked.asStateFlow()

    // CRUD - Folder operations
    fun createFolder(name: String, colorHex: String = "#4F46E5") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFolder(Folder(name = name, colorHex = colorHex))
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
            repository.deleteFolder(folder)
        }
    }

    fun selectFolder(folderId: Int?) {
        _selectedFolderId.value = folderId
    }

    // CRUD - Notes operations
    fun createNote(title: String, content: String, folderId: Int? = null, isPinned: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(
                Note(
                    title = title.trim().ifEmpty { "Untitled Note" },
                    content = content,
                    folderId = folderId,
                    isPinned = isPinned,
                    createdAt = System.currentTimeMillis(),
                    lastModifiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(note.copy(lastModifiedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun moveNoteToFolder(note: Note, folderId: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(note.copy(folderId = folderId))
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // CRUD - Distraction filters actions
    fun toggleFilterEnabled(filter: DistractionFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = filter.copy(isEnabled = !filter.isEnabled)
            repository.updateFilter(updated)
            // Dynamically refresh block packages if active
            refreshNotificationBlocker(updated)
        }
    }

    fun addDistractionFilter(type: String, name: String, target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val filterTarget = target.trim().lowercase().removePrefix("http://").removePrefix("https://").removePrefix("www.")
            repository.insertFilter(DistractionFilter(type = type, name = name, target = filterTarget, isEnabled = true))
        }
    }

    fun deleteFilter(filter: DistractionFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFilter(filter)
        }
    }

    private fun refreshNotificationBlocker(updated: DistractionFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = repository.getActiveFilters()
            val apps = active.filter { it.type == "app" }.map { it.target }.toSet()
            FocusNotificationService.blockedPackages = apps
        }
    }

    // STUDY TIMER TRIGGERS
    fun startStudySession(context: Context, minutes: Int, note: Note?) {
        _linkedNote.value = note
        _studyTimeTotal.value = minutes * 60L
        _studyTimeRemaining.value = minutes * 60L
        _isStudyModeActive.value = true

        val intent = Intent(context, StudySessionService::class.java).apply {
            action = "START"
            putExtra("duration", minutes)
            note?.let {
                putExtra("noteId", it.id)
                putExtra("noteTitle", it.title)
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun updateStudyProgress(remaining: Long) {
        _studyTimeRemaining.value = remaining
        _isStudyModeActive.value = remaining > 0
    }

    fun finishStudySession() {
        _isStudyModeActive.value = false
        _studyTimeRemaining.value = 0
        _linkedNote.value = null
    }

    fun stopStudySession(context: Context) {
        _isStudyModeActive.value = false
        _studyTimeRemaining.value = 0
        _linkedNote.value = null
        
        val intent = Intent(context, StudySessionService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }

    fun pauseStudySession(context: Context) {
        val intent = Intent(context, StudySessionService::class.java).apply {
            action = "PAUSE"
        }
        context.startService(intent)
    }

    fun resumeStudySession(context: Context) {
        val intent = Intent(context, StudySessionService::class.java).apply {
            action = "RESUME"
        }
        context.startService(intent)
    }

    // EXTERNAL DISTRACTION OVERLAY ACTIONS
    fun triggerBlockedAppNotification(packageName: String?) {
        _blockedAppTrigger.value = packageName
    }

    fun clearBlockedAppTrigger() {
        _blockedAppTrigger.value = null
    }

    // IN-APP INTEGRATED STUDY RESEARCH WEB BROWSER
    fun navigateWebPortal(address: String) {
        val cleanUrl = address.trim().lowercase()
        _activeWebViewUrl.value = address

        // Evaluate distraction filter matches
        viewModelScope.launch(Dispatchers.IO) {
            val activeWebFilters = repository.getActiveFilters().filter { it.type == "website" }
            val match = activeWebFilters.firstOrNull { filter ->
                cleanUrl.contains(filter.target) || filter.target.contains(cleanUrl)
            }
            if (match != null && _isStudyModeActive.value) {
                // Instantly block URL and increment block statistics
                repository.incrementBlockCount(match.id)
                _isDistractingWebBlocked.value = true
            } else {
                _isDistractingWebBlocked.value = false
            }
        }
    }

    fun closeWebPortal() {
        _activeWebViewUrl.value = null
        _isDistractingWebBlocked.value = false
    }

    fun dismissWebBlockOverlay() {
        _isDistractingWebBlocked.value = false
        _activeWebViewUrl.value = "https://www.google.com" // Redirect study to general resources
    }
}

class StudyViewModelFactory(private val repository: StudyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
