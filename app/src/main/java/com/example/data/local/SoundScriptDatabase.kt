package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LectureNoteEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SoundScriptDatabase : RoomDatabase() {
    abstract fun lectureNoteDao(): LectureNoteDao

    companion object {
        @Volatile
        private var INSTANCE: SoundScriptDatabase? = null

        fun getInstance(context: Context): SoundScriptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundScriptDatabase::class.java,
                    "soundscript_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
