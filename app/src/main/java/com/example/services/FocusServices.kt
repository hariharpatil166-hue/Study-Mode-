package com.example.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.DistractionFilter
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Clean service to intercept and block incoming social media notifications
 */
class FocusNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (isStudyModeActive) {
            val packageName = sbn.packageName
            if (blockedPackages.contains(packageName)) {
                // Cancel (dismiss) the notification automatically!
                cancelNotification(sbn.key)
                
                // Track stats in background
                val database = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        database.distractionFilterDao().incrementBlockCountByTarget(packageName)
                    } catch (e: Exception) {
                        Log.e("NotificationBlock", "Failed to update count", e)
                    }
                }
                
                // Notify listeners
                onAppBlocked?.invoke(packageName)
            }
        }
    }

    companion object {
        var isStudyModeActive = false
        var blockedPackages = setOf<String>()
        var onAppBlocked: ((String) -> Unit)? = null
    }
}

/**
 * Foreground Service that manages the active countdown timer,
 * updates the study notification, and monitors foreground apps to run distraction blocking.
 */
class StudySessionService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var monitorJob: Job? = null
    
    var timeRemainingSeconds = 0L
        private set
        
    var totalSessionSeconds = 0L
        private set
        
    var isTimerRunning = false
        private set

    var currentNoteTitle: String? = null
    var currentNoteId: Int? = null

    private val channelId = "study_mode_channel"
    private val notificationId = 1001

    inner class LocalBinder : Binder() {
        fun getService(): StudySessionService = this@StudySessionService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val durationMinutes = intent?.getIntExtra("duration", 25) ?: 25
        val noteId = intent?.getIntExtra("noteId", -1)?.takeIf { it != -1 }
        val noteTitle = intent?.getStringExtra("noteTitle")

        when (action) {
            "START" -> startStudySession(durationMinutes, noteId, noteTitle)
            "STOP" -> stopStudySession()
            "PAUSE" -> pauseStudySession()
            "RESUME" -> resumeStudySession()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Study Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps you focused and tracks active study timers"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Study Session Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startStudySession(minutes: Int, noteId: Int?, noteTitle: String?) {
        currentNoteId = noteId
        currentNoteTitle = noteTitle
        totalSessionSeconds = minutes * 60L
        timeRemainingSeconds = totalSessionSeconds
        isTimerRunning = true

        FocusNotificationService.isStudyModeActive = true
        loadBlockedAppsAndWebsites()

        startForeground(notificationId, buildNotification("Focusing on your study..."))
        
        startTimer()
        startAppMonitor()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (timeRemainingSeconds > 0) {
                delay(1000)
                timeRemainingSeconds--
                updateNotification()
                
                // Broadcast ticks
                val intent = Intent("TIMER_TICK").apply {
                    putExtra("remaining", timeRemainingSeconds)
                }
                sendBroadcast(intent)
            }
            onSessionFinished()
        }
    }

    private fun startAppMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.IO) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive) {
                delay(1200) // Poll frequently to block promptly
                if (isTimerRunning && usageStatsManager != null) {
                    val time = System.currentTimeMillis()
                    val stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        time - 2000,
                        time
                    )
                    
                    if (!stats.isNullOrEmpty()) {
                        // Find most recently used foreground package
                        val foregroundApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName
                        if (foregroundApp != null && foregroundApp != packageName) {
                            if (FocusNotificationService.blockedPackages.contains(foregroundApp)) {
                                // Increment block count in Room
                                val database = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
                                database.distractionFilterDao().incrementBlockCountByTarget(foregroundApp)
                                
                                // Fire block intent to break user concentration and open focus lock
                                triggerBlockScreenOverlay(foregroundApp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerBlockScreenOverlay(blockedPkg: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("BLOCKED_APP_TRIGGER", blockedPkg)
        }
        startActivity(intent)
    }

    private fun loadBlockedAppsAndWebsites() {
        val db = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
        scope.launch(Dispatchers.IO) {
            val active = db.distractionFilterDao().getActiveFilters()
            val apps = active.filter { it.type == "app" }.map { it.target }.toSet()
            FocusNotificationService.blockedPackages = apps
        }
    }

    private fun updateNotification() {
        val minutesLeft = timeRemainingSeconds / 60
        val secondsLeft = timeRemainingSeconds % 60
        val text = if (currentNoteTitle != null) {
            "Lecture: $currentNoteTitle - ${minutesLeft}m ${secondsLeft}s left"
        } else {
            "Timer active: ${minutesLeft}m ${secondsLeft}s left"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, buildNotification(text))
    }

    private fun pauseStudySession() {
        isTimerRunning = false
        timerJob?.cancel()
        updateNotification()
    }

    private fun resumeStudySession() {
        isTimerRunning = true
        startTimer()
    }

    private fun stopStudySession() {
        FocusNotificationService.isStudyModeActive = false
        isTimerRunning = false
        timerJob?.cancel()
        monitorJob?.cancel()
        
        // Save incomplete study session stats
        saveSessionToDatabase(false)
        
        stopSelf()
    }

    private fun onSessionFinished() {
        FocusNotificationService.isStudyModeActive = false
        isTimerRunning = false
        timerJob?.cancel()
        monitorJob?.cancel()
        
        saveSessionToDatabase(true)
        
        val intent = Intent("TIMER_FINISHED")
        sendBroadcast(intent)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun saveSessionToDatabase(completed: Boolean) {
        val elapsedMinutes = ((totalSessionSeconds - timeRemainingSeconds) / 60).toInt().coerceAtLeast(1)
        val db = AppDatabase.getDatabase(applicationContext, CoroutineScope(Dispatchers.IO))
        
        scope.launch(Dispatchers.IO) {
            try {
                db.studySessionDao().insertSession(
                    com.example.data.StudySession(
                        noteId = currentNoteId,
                        noteTitle = currentNoteTitle,
                        durationMinutes = elapsedMinutes,
                        completed = completed
                    )
                )
            } catch (e: Exception) {
                Log.e("StudySessionService", "Error saving session", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusNotificationService.isStudyModeActive = false
        timerJob?.cancel()
        monitorJob?.cancel()
        scope.cancel()
    }
}
