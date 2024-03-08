package com.example.pdfnotemate.room

import com.example.pdfnotemate.room.entity.PdfNoteEntity

object Queries {
    /**[PdfNoteEntity]*/
    const val GET_ALL_PDF_NOTES = "SELECT * FROM ${PdfNoteEntity.TABLE_NAME}"
}