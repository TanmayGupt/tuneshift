package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedPlaylistEntity::class,
        TrackItemEntity::class,
        MigrationHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TuneShiftDatabase : RoomDatabase() {
    abstract fun tuneShiftDao(): TuneShiftDao

    companion object {
        @Volatile
        private var INSTANCE: TuneShiftDatabase? = null

        fun getInstance(context: Context): TuneShiftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TuneShiftDatabase::class.java,
                    "tuneshift_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
