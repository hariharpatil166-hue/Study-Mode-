package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.StudyRepository
import com.example.ui.StudyAppUI
import com.example.ui.registerShareActivityCallback
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StudyViewModel
import com.example.viewmodel.StudyViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: StudyViewModel

    // Tick broadcast receiver to keep timer UI aligned with background foreground Service count
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                "TIMER_TICK" -> {
                    val remaining = intent.getLongExtra("remaining", 0L)
                    viewModel.updateStudyProgress(remaining)
                }
                "TIMER_FINISHED" -> {
                    viewModel.finishStudySession()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Local Database & Repositories
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = StudyRepository(
            database.folderDao(),
            database.noteDao(),
            database.studySessionDao(),
            database.distractionFilterDao()
        )

        // 2. Build Lifecycle VM State
        val factory = StudyViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StudyViewModel::class.java]

        // 3. Capture study block screen overlay intent triggers
        intent?.getStringExtra("BLOCKED_APP_TRIGGER")?.let { blockedApp ->
            viewModel.triggerBlockedAppNotification(blockedApp)
        }

        // 4. Hook Class notes / lecture System Share sheets
        registerShareActivityCallback { title, content, link ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, content)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share notes to others")
            startActivity(shareIntent)
        }

        // 5. Register study-timer broadcast receivers
        val filter = IntentFilter().apply {
            addAction("TIMER_TICK")
            addAction("TIMER_FINISHED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(timerReceiver, filter)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Inner elements handle edge-to-edge padding where necessary
                    StudyAppUI(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("BLOCKED_APP_TRIGGER")?.let { blockedApp ->
            viewModel.triggerBlockedAppNotification(blockedApp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(timerReceiver)
        } catch (e: Exception) {
            // Graceful safe bypass
        }
    }
}
