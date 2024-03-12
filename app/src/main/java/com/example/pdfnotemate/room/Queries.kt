package com.example.pdfnotemate.room

import com.example.pdfnotemate.room.entity.BookmarkEntity
import com.example.pdfnotemate.room.entity.CommentEntity
import com.example.pdfnotemate.room.entity.HighlightEntity
import com.example.pdfnotemate.room.entity.PdfNoteEntity
import com.example.pdfnotemate.room.entity.PdfTagEntity

object Queries {
    // PDF
    const val GET_ALL_PDF_NOTES = "SELECT * FROM ${PdfNoteEntity.TABLE_NAME}"

    // TAG
    const val GET_TAG_BY_TAG_ID =
        "SELECT * FROM ${PdfTagEntity.TABLE_NAME} WHERE ${PdfTagEntity.FIELD_ID} = :tagId"
    const val GET_ALL_TAGS =
        "SELECT * FROM ${PdfTagEntity.TABLE_NAME}"

    const val REMOVE_TAG_BY_ID =
        "DELETE FROM ${PdfTagEntity.TABLE_NAME} WHERE ${PdfTagEntity.FIELD_ID} = :tagId"

    // COMMENT
    const val GET_ALL_COMMENTS_WITH_PDF_ID =
        "SELECT * FROM ${CommentEntity.TABLE_NAME} WHERE ${CommentEntity.FIELD_PDF_ID}=:pdfId"
    const val DELETE_COMMENTS_WITH_IDS =
        "DELETE FROM ${CommentEntity.TABLE_NAME} WHERE ${CommentEntity.FIELD_PDF_ID} IN (:ids)"

    // HIGHLIGHT
    const val GET_ALL_HIGHLIGHT_WITH_PDF_ID =
        "SELECT * FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_PDF_ID}=:pdfId"
    const val DELETE_HIGHLIGHTS_WITH_IDS =
        "DELETE FROM ${HighlightEntity.TABLE_NAME} WHERE ${HighlightEntity.FIELD_PDF_ID} IN (:ids)"

    // BOOKMARK
    const val GET_ALL_BOOKMARK_WITH_PDF_ID =
        "SELECT * FROM ${BookmarkEntity.TABLE_NAME} WHERE ${BookmarkEntity.FIELD_PDF_ID}=:pdfId"
    const val DELETE_BOOKMARK_WITH_IDS =
        "DELETE FROM ${BookmarkEntity.TABLE_NAME} WHERE ${BookmarkEntity.FIELD_PDF_ID} IN (:ids)"
    const val GET_BOOKMARK_WITH_PAGE_AND_PDF_ID =
        "SELECT * FROM ${BookmarkEntity.TABLE_NAME} WHERE ${BookmarkEntity.FIELD_PAGE}=:page AND ${BookmarkEntity.FIELD_PDF_ID}=:pdfId"


}