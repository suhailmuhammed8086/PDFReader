package com.example.pdfnotemate.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pdfnotemate.room.entity.PdfNoteEntity
import com.example.pdfnotemate.room.entity.PdfTagEntity

@Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPdfNote(note: PdfNoteEntity) : Long
    @Query(Queries.GET_ALL_PDF_NOTES)
    suspend fun getAllPdfNotes(): List<PdfNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPdfTag(tag: PdfTagEntity): Long

    @Query(Queries.GET_TAG_BY_TAG_ID)
    suspend fun getTagById(tagId:Long): PdfTagEntity?
}