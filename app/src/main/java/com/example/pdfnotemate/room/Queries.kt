package com.example.pdfnotemate.room

import com.example.pdfnotemate.room.entity.PdfNoteEntity
import com.example.pdfnotemate.room.entity.PdfTagEntity

object Queries {
    /**[PdfNoteEntity]*/
    const val GET_ALL_PDF_NOTES = "SELECT * FROM ${PdfNoteEntity.TABLE_NAME}"
    const val GET_TAG_BY_TAG_ID = "SELECT * FROM ${PdfTagEntity.TABLE_NAME} WHERE ${PdfTagEntity.FIELD_ID} = :tagId"
}