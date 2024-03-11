package com.example.pdfnotemate.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pdfnotemate.room.entity.CommentEntity
import com.example.pdfnotemate.room.entity.HighlightEntity
import com.example.pdfnotemate.room.entity.BookmarkEntity
import com.example.pdfnotemate.room.entity.PdfNoteEntity
import com.example.pdfnotemate.room.entity.PdfTagEntity

@Database(
    [
        PdfNoteEntity::class,
        PdfTagEntity::class,
        CommentEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
    ], version = AppDatabase.VERSION
)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getDao(): Dao

    companion object {
        private const val DATABASE_NAME = "app_database"
        const val VERSION = 2

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