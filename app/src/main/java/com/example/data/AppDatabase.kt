package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Folder::class, Note::class, StudySession::class, DistractionFilter::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun distractionFilterDao(): DistractionFilterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_focus_notes_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.distractionFilterDao())
                }
            }
        }

        private suspend fun populateDatabase(filterDao: DistractionFilterDao) {
            // Pre-seed common social media apps
            filterDao.insertFilter(DistractionFilter(type = "app", name = "Instagram", target = "com.instagram.android"))
            filterDao.insertFilter(DistractionFilter(type = "app", name = "Facebook", target = "com.facebook.katana"))
            filterDao.insertFilter(DistractionFilter(type = "app", name = "Twitter / X", target = "com.twitter.android"))
            filterDao.insertFilter(DistractionFilter(type = "app", name = "TikTok", target = "com.zhiliaoapp.musically"))
            filterDao.insertFilter(DistractionFilter(type = "app", name = "Reddit", target = "com.reddit.frontpage"))
            filterDao.insertFilter(DistractionFilter(type = "app", name = "YouTube", target = "com.google.android.youtube"))

            // Pre-seed common distracting websites
            filterDao.insertFilter(DistractionFilter(type = "website", name = "Instagram", target = "instagram.com"))
            filterDao.insertFilter(DistractionFilter(type = "website", name = "Facebook", target = "facebook.com"))
            filterDao.insertFilter(DistractionFilter(type = "website", name = "Twitter / X", target = "twitter.com"))
            filterDao.insertFilter(DistractionFilter(type = "website", name = "X.com", target = "x.com"))
            filterDao.insertFilter(DistractionFilter(type = "website", name = "TikTok", target = "tiktok.com"))
            filterDao.insertFilter(DistractionFilter(type = "website", name = "Reddit", target = "reddit.com"))
            filterDao.insertFilter(DistractionFilter(type = "website", name = "YouTube", target = "youtube.com"))
        }
    }
}
