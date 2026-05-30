package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DistractionFilter
import com.example.data.Folder
import com.example.data.Note
import com.example.data.StudySession
import com.example.viewmodel.StudyViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAppUI(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }
    
    // Bottom navigation items
    val tabItems = listOf(
        TabItem("Notes", Icons.Outlined.Description, Icons.Filled.Description),
        TabItem("Study Timer", Icons.Outlined.Timer, Icons.Filled.Timer),
        TabItem("Filters/Blocks", Icons.Outlined.AppSettingsAlt, Icons.Filled.AppSettingsAlt),
        TabItem("Research", Icons.Outlined.TravelExplore, Icons.Filled.TravelExplore)
    )

    // State flows from ViewModel
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val isStudyModeActive by viewModel.isStudyModeActive.collectAsStateWithLifecycle()
    val studyTimeRemaining by viewModel.studyTimeRemaining.collectAsStateWithLifecycle()
    val studyTimeTotal by viewModel.studyTimeTotal.collectAsStateWithLifecycle()
    val linkedNote by viewModel.linkedNote.collectAsStateWithLifecycle()
    val blockedAppTrigger by viewModel.blockedAppTrigger.collectAsStateWithLifecycle()
    
    // Editor State
    var showNoteEditor by remember { mutableStateOf<Note?>(null) }
    var isNewNote by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalActivity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Study Focus Notes",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                actions = {
                    if (isStudyModeActive) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OfflineBolt,
                                    contentDescription = "Active Study Mode",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FOCUS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = BottomNavBg,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFFE7E0EC),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                },
                windowInsets = WindowInsets.navigationBars
            ) {
                tabItems.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimaryContainer,
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        ),
                        icon = {
                            Icon(
                                imageVector = if (activeTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = {
                        showNoteEditor = Note(title = "", content = "", folderId = viewModel.selectedFolderId.value)
                        isNewNote = true
                    },
                    modifier = Modifier.testTag("add_note_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Note")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Tab Switching
            when (activeTab) {
                0 -> NotesTab(
                    viewModel = viewModel,
                    folders = folders,
                    notes = filteredNotes,
                    onNoteClick = { note ->
                        showNoteEditor = note
                        isNewNote = false
                    }
                )
                1 -> StudyTimerTab(
                    viewModel = viewModel,
                    notes = filteredNotes,
                    isStudyModeActive = isStudyModeActive,
                    studyTimeRemaining = studyTimeRemaining,
                    studyTimeTotal = studyTimeTotal,
                    linkedNote = linkedNote
                )
                2 -> BlocksSettingsTab(
                    viewModel = viewModel
                )
                3 -> WebResearchTab(
                    viewModel = viewModel,
                    isStudyModeActive = isStudyModeActive
                )
            }

            // Lock Overlay if User Opened Distracting App
            blockedAppTrigger?.let { packageName ->
                DistractionLockPopup(
                    packageName = packageName,
                    onDismiss = { viewModel.clearBlockedAppTrigger() }
                )
            }
        }
    }

    // Note Editor Dialog
    showNoteEditor?.let { note ->
        NoteEditorDialog(
            note = note,
            folders = folders,
            isNewNote = isNewNote,
            onSave = { updatedNote ->
                if (isNewNote) {
                    viewModel.createNote(
                        title = updatedNote.title,
                        content = updatedNote.content,
                        folderId = updatedNote.folderId,
                        isPinned = updatedNote.isPinned
                    )
                } else {
                    viewModel.updateNote(updatedNote)
                }
                showNoteEditor = null
            },
            onDelete = {
                viewModel.deleteNote(note)
                showNoteEditor = null
            },
            onDismiss = { showNoteEditor = null }
        )
    }
}

data class TabItem(
    val label: String,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

// ==========================================
// NOTES & LECTURES MODULE
// ==========================================
@Composable
fun NotesTab(
    viewModel: StudyViewModel,
    folders: List<Folder>,
    notes: List<Note>,
    onNoteClick: (Note) -> Unit
) {
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    var showAddFolderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search lectures, notes, or topics...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_notes_input")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Folders Section Title with Add Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLASS FOLDERS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { showAddFolderDialog = true },
                modifier = Modifier.testTag("add_folder_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CreateNewFolder,
                    contentDescription = "New Folder",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Horizontal Folders List
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { viewModel.selectFolder(null) },
                    label = { Text("All Notes") },
                    leadingIcon = { Icon(Icons.Filled.FolderCopy, "All") }
                )
            }
            items(folders) { folder ->
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = { viewModel.selectFolder(folder.id) },
                    label = { Text(folder.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = folder.name,
                            tint = Color(android.graphics.Color.parseColor(folder.colorHex))
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.deleteFolder(folder) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Filled.Close, "Delete", modifier = Modifier.size(12.dp))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes List Container
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPasteOff,
                        contentDescription = "No notes found",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matches found" else "No lecture notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try searching a different keyword." else "Create your first class topic, study guideline, or lecture summary using the (+) button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Pin list first
                val sortedNotes = notes.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.lastModifiedAt })
                items(sortedNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        folder = folders.firstOrNull { it.id == note.folderId },
                        onClick = { onNoteClick(note) },
                        onPinToggle = { viewModel.togglePinNote(note) },
                        onShare = { shareNoteText(viewModel, note) }
                    )
                }
            }
        }
    }

    // Dialog to create folder
    if (showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = { showAddFolderDialog = false },
            onAddFolder = { name, color ->
                viewModel.createFolder(name, color)
                showAddFolderDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    folder: Folder?,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onShare: () -> Unit
) {
    val isPinned = note.isPinned
    val containerColor = if (isPinned) {
        if (note.id % 2L == 0L) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isPinned) {
        if (note.id % 2L == 0L) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isPinned) null else CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    folder?.let { Color(android.graphics.Color.parseColor(it.colorHex)) } ?: MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.colorScheme.outlineVariant
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPinned) 1.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("pin_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Folder Badge
                if (folder != null) {
                    val color = Color(android.graphics.Color.parseColor(folder.colorHex))
                    Surface(
                        color = if (isPinned) contentColor.copy(alpha = 0.12f) else color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = folder.name,
                            color = if (isPinned) contentColor else color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = note.content,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }

            // Share and details footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(note.lastModifiedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
                
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("share_note_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Lecture Note",
                        tint = if (isPinned) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Format Time helper
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// share payload trigger
private fun shareNoteText(viewModel: StudyViewModel, note: Note) {
    // Generate a beautiful simulated share token
    val shareToken = "https://learnflow.link/share/note_${note.id}_" + UUID.randomUUID().toString().take(6)
    
    // Also use the real context to invoke an Android share sheet
    // This provides standard direct system share as requested
    val shareMsg = """
        📖 Class Lecture Shared from Study Focus Notes:
        📌 Topic: ${note.title}
        
        ${note.content}
        
        🔗 Read Study Portal Note: $shareToken
    """.trimIndent()
    
    _shareTriggerEvent?.invoke(note.title, shareMsg, shareToken)
}

// Temporary global event for Activity to capture Compose share callbacks
private var _shareTriggerEvent: ((String, String, String) -> Unit)? = null
fun registerShareActivityCallback(callback: (String, String, String) -> Unit) {
    _shareTriggerEvent = callback
}


// ==========================================
// STUDY TIMER MODULE
// ==========================================
@Composable
fun StudyTimerTab(
    viewModel: StudyViewModel,
    notes: List<Note>,
    isStudyModeActive: Boolean,
    studyTimeRemaining: Long,
    studyTimeTotal: Long,
    linkedNote: Note?
) {
    val context = LocalContext.current
    var inputTimerMinutes by remember { mutableStateOf(25) }
    var selectedNoteToLink by remember { mutableStateOf<Note?>(null) }
    var showLinkDropdown by remember { mutableStateOf(false) }

    val studyHistory by viewModel.studyHistory.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Learning Focus Studio",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Block notifications & stay locked into academic goals",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = FocusModeBg,
                contentColor = FocusModeText
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radial timer ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(190.dp)
                        .padding(8.dp)
                ) {
                    val progress = remember(studyTimeRemaining, studyTimeTotal) {
                        if (studyTimeTotal > 0) studyTimeRemaining.toFloat() / studyTimeTotal else 0f
                    }

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
                        label = "timer_progress"
                    )

                    val ringColor = FocusModeAccent
                    val trackColor = FocusModeSubBg

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTimerTime(studyTimeRemaining.takeIf { isStudyModeActive } ?: (inputTimerMinutes * 60L)),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isStudyModeActive) "FOCUSED RUNNING" else "CHOOSE TIMER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FocusModeText.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isStudyModeActive) {
                    // Preset row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            val isSelected = inputTimerMinutes == mins
                            Button(
                                onClick = { inputTimerMinutes = mins },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) FocusModeAccent else FocusModeSubBg,
                                    contentColor = if (isSelected) FocusModeBg else FocusModeText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("preset_timer_${mins}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dropdown link button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showLinkDropdown = true },
                            border = BorderStroke(1.dp, FocusModeAccent.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = FocusModeText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_lecture_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(16.dp), tint = FocusModeAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedNoteToLink?.let { "Linked Note: ${it.title}" } ?: "Optional: Link a lecture/note",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FocusModeText
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = FocusModeAccent)
                            }
                        }

                        DropdownMenu(
                            expanded = showLinkDropdown,
                            onDismissRequest = { showLinkDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(FocusModeSubBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None", color = Color.White) },
                                onClick = {
                                    selectedNoteToLink = null
                                    showLinkDropdown = false
                                }
                            )
                            notes.forEach { note ->
                                DropdownMenuItem(
                                    text = { Text(note.title, color = Color.White) },
                                    onClick = {
                                        selectedNoteToLink = note
                                        showLinkDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Action Button
                    Button(
                        onClick = {
                            viewModel.startStudySession(context, inputTimerMinutes, selectedNoteToLink)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_timer_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FocusModeAccent,
                            contentColor = FocusModeBg
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start Timer")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Study Session Mode", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Running state inside the elegant purple card!
                    linkedNote?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FocusModeSubBg)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "Focus Notes Target",
                                    tint = FocusModeAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Linked Session Lecture",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FocusModeAccent
                                    )
                                    Text(
                                        it.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.pauseStudySession(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusModeSubBg, contentColor = FocusModeText),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = "Pause Session")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.resumeStudySession(context) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusModeSubBg, contentColor = FocusModeText),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = "Resume Session")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.stopStudySession(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stop_timer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop focusing")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop & Record Session")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History logs
        Text(
            text = "Active Study Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        if (studyHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No study logs recorded yet. Begin a session to populate dynamic progress tracking.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(studyHistory) { session ->
                    StudyHistoryRow(session = session)
                }
            }
        }
    }
}

@Composable
fun StudyHistoryRow(session: StudySession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.noteTitle?.let { "Studied: $it" } ?: "General Focus Block Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(session.startTime),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (session.completed) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "${session.durationMinutes} min - " + if (session.completed) "Success" else "Incomplete",
                    color = if (session.completed) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun formatTimerTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}


// ==========================================
// BLOCK SETTINGS & SYSTEM PERMISSIONS
// ==========================================
@Composable
fun BlocksSettingsTab(
    viewModel: StudyViewModel
) {
    val filters by viewModel.distractionFilters.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inputCustomName by remember { mutableStateOf("") }
    var inputCustomPackageWeb by remember { mutableStateOf("") }
    var filterTypeTabIndex by remember { mutableStateOf(0) } // 0 = App, 1 = Web

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PERMISSIONS AND SYSTEM ACCESS CARDS
        item {
            Text(
                "System Access Permissions Setup",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            PermissionIntegrationInfoCard()
        }

        // CUSTOM BLOCK ADDITION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Custom Block List Target",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TabRow(
                        selectedTabIndex = filterTypeTabIndex,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Tab(
                            selected = filterTypeTabIndex == 0,
                            onClick = { filterTypeTabIndex = 0 },
                            text = { Text("App Package Block", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = filterTypeTabIndex == 1,
                            onClick = { filterTypeTabIndex = 1 },
                            text = { Text("Website Domain Block", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputCustomName,
                        onValueChange = { inputCustomName = it },
                        placeholder = { Text("Friendly Display Name (e.g. Instagram)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputCustomPackageWeb,
                        onValueChange = { inputCustomPackageWeb = it },
                        placeholder = { 
                            Text(
                                if (filterTypeTabIndex == 0) "Package Name (e.g. com.instagram.android)" 
                                else "Domain URL (e.g. instagram.com)"
                            ) 
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (inputCustomName.isEmpty() || inputCustomPackageWeb.isEmpty()) {
                                Toast.makeText(context, "Fill out both names to insert filter block.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addDistractionFilter(
                                type = if (filterTypeTabIndex == 0) "app" else "website",
                                name = inputCustomName,
                                target = inputCustomPackageWeb
                            )
                            inputCustomName = ""
                            inputCustomPackageWeb = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to Blocklist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // BLOCKED ITEM SWITCHERS
        item {
            Text(
                "Configured Filters",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val appFilters = filters.filter { it.type == "app" }
        if (appFilters.isNotEmpty()) {
            item {
                Text("App Package Focus Blocker", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(appFilters) { filter ->
                FilterRowItem(filter = filter, onToggle = { viewModel.toggleFilterEnabled(filter) }, onDelete = { viewModel.deleteFilter(filter) })
            }
        }

        val webFilters = filters.filter { it.type == "website" }
        if (webFilters.isNotEmpty()) {
            item {
                Text("Web Research Blocked Domains", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(webFilters) { filter ->
                FilterRowItem(filter = filter, onToggle = { viewModel.toggleFilterEnabled(filter) }, onDelete = { viewModel.deleteFilter(filter) })
            }
        }
    }
}

@Composable
fun FilterRowItem(
    filter: DistractionFilter,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (filter.type == "app") Icons.Filled.PhoneAndroid else Icons.Filled.Web,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(filter.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    filter.target,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (filter.blockCount > 0) {
                    Text(
                        "Blocked ${filter.blockCount} times!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = filter.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("toggle_block_${filter.id}")
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, "Delete Filter", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PermissionIntegrationInfoCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.5f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Required Permissions Guard",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "To intercept incoming notifications and block foreground social media app switches, Android sandboxing requires system permission authorization:",
                fontSize = 11.sp,
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Usage Access
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("1. Apps Usage Stats access", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Allows checking if you opened blocked apps like Instagram.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Search for Usage Access in system Settings.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Authorize", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notification Listener Access
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("2. Notification interception", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Allows dismissing social notifications while Study Mode is active.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Find notification access in settings manually.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Authorize", fontSize = 10.sp)
                }
            }
        }
    }
}


// ==========================================
// INTEGRATED WEB RESEARCH PORTAL
// ==========================================
@Composable
fun WebResearchTab(
    viewModel: StudyViewModel,
    isStudyModeActive: Boolean
) {
    val activeWebViewUrl by viewModel.activeWebViewUrl.collectAsStateWithLifecycle()
    val isDistractingWebBlocked by viewModel.isDistractingWebBlocked.collectAsStateWithLifecycle()
    
    var localUrlAddressBar by remember { mutableStateOf("https://www.google.com") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = localUrlAddressBar,
                    onValueChange = { localUrlAddressBar = it },
                    placeholder = { Text("Enter URL (e.g. wikipedia.org)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.navigateWebPortal(localUrlAddressBar)
                    }),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.navigateWebPortal(localUrlAddressBar) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Go text url")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
        ) {
            if (activeWebViewUrl == null) {
                // Intro Home Screen of Study Research Portal
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Study Web Research Shield",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Search study articles, online manuals, or open textbooks. The built-in guard automatically filters and blocks distracting social domains when your Study Timer is running!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { 
                            localUrlAddressBar = "https://www.wikipedia.org"
                            viewModel.navigateWebPortal(localUrlAddressBar)
                        }) {
                            Text("Wikipedia")
                        }
                        Button(onClick = { 
                            localUrlAddressBar = "https://scholar.google.com"
                            viewModel.navigateWebPortal(localUrlAddressBar)
                        }) {
                            Text("Google Scholar")
                        }
                    }
                }
            } else if (isDistractingWebBlocked && isStudyModeActive) {
                // Block screen overlay inside the app
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Website Blocked",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Distraction Filter Activated!",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This website category is restricted by your Study Focus Blocklist while the study session is countdown running. Stay locked in on your academic success!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { 
                                viewModel.dismissWebBlockOverlay()
                                localUrlAddressBar = "https://www.google.com"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Return to Research Portal")
                        }
                    }
                }
            } else {
                // Real Working WebView
                AndroidWebViewWidget(
                    url = activeWebViewUrl!!,
                    onUrlInterceptedBlock = { interceptedUrl ->
                        viewModel.navigateWebPortal(interceptedUrl)
                    }
                )
            }
        }
    }
}

@Composable
fun AndroidWebViewWidget(
    url: String,
    onUrlInterceptedBlock: (String) -> Unit
) {
    val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        onUrlInterceptedBlock(requestUrl)
                        return false // Allow loading unless intercepted block sets redirect state
                    }
                }
                settings.javaScriptEnabled = true
                loadUrl(cleanUrl)
            }
        },
        update = { webView ->
            if (webView.url != cleanUrl) {
                webView.loadUrl(cleanUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


// ==========================================
// LOCK OVERLAY SCREEN
// ==========================================
@Composable
fun DistractionLockPopup(
    packageName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.DoNotDisturbOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FOCUS BLOCK ACTIVE!",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val cleanAppName = packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                
                Text(
                    text = "We detected that you attempted to open the distracting application '$cleanAppName'. Get back to your workspace and complete your lecture notes study sequence!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Resume Focused Study Session", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}


// ==========================================
// EDITOR AND FOLDER DIALOG CREATORS
// ==========================================
@Composable
fun NoteEditorDialog(
    note: Note,
    folders: List<Folder>,
    isNewNote: Boolean,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var folderId by remember { mutableStateOf(note.folderId) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var showFolderSelectDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNewNote) "New Lecture Note" else "View / Edit Note",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin important",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title / Lecture Subject") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_editor_title")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Folder Association Selector
                    Box {
                        OutlinedButton(
                            onClick = { showFolderSelectDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val associatedFolder = folders.firstOrNull { it.id == folderId }
                            Text(associatedFolder?.let { "Folder: ${it.name}" } ?: "Assign to Class Folder")
                        }
                        DropdownMenu(
                            expanded = showFolderSelectDropdown,
                            onDismissRequest = { showFolderSelectDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("(Uncategorized)") },
                                onClick = {
                                    folderId = null
                                    showFolderSelectDropdown = false
                                }
                            )
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        folderId = folder.id
                                        showFolderSelectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Write your lecture insights, notes, or lists...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("note_editor_content")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isNewNote) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_note_button")
                        ) {
                            Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                note.copy(
                                    title = title,
                                    content = content,
                                    folderId = folderId,
                                    isPinned = isPinned
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_note_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onAddFolder: (String, String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val colorsHex = listOf("#4F46E5", "#10B981", "#EF4444", "#F59E0B", "#8B5CF6")
    var selectedColorIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Create Class Folder",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder Name (e.g. Physics, Chemistry)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_folder_title_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Folder Color", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorsHex.forEachIndexed { index, colorStr ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorStr)))
                                .clickable { selectedColorIndex = index }
                                .drawBehind {
                                    if (selectedColorIndex == index) {
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension / 4f,
                                            center = center
                                        )
                                    }
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (folderName.isNotEmpty()) {
                                onAddFolder(folderName, colorsHex[selectedColorIndex])
                            }
                        },
                        modifier = Modifier.testTag("save_folder_button")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
