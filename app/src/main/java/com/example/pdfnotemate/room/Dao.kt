package com.example.pdfnotemate.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pdfnotemate.room.entity.PdfNoteEntity

@Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPdfNote(note: PdfNoteEntity) : Long
    @Query(Queries.GET_ALL_PDF_NOTES)
    fun getAllPdfNotes(): List<PdfNoteEntity>
}