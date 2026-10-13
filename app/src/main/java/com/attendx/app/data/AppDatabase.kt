package com.attendx.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for AttendX. Single table: subjects.
 * Schema export is disabled ([exportSchema] = false) since this is a
 * single-version demo with no migrations to track.
 */
@Database(entities = [Subject::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao

    companion object {
        private const val DB_NAME = "attendx_db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
