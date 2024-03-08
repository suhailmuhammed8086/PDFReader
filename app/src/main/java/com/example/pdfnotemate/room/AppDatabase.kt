package com.example.pdfnotemate.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pdfnotemate.room.entity.PdfNoteEntity

@Database([PdfNoteEntity::class], version = AppDatabase.VERSION)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "app_database"
        const val VERSION = 1

        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance != null) {
                return instance!!
            }

            instance = Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()

            return instance!!
        }
    }
}