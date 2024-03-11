package com.example.pdfnotemate.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pdfnotemate.tools.pdf.viewer.model.Coordinates

@Entity(tableName = CommentEntity.TABLE_NAME)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long? = null,

    @ColumnInfo(name = FIELD_PDF_ID)
    val pdfId: Long = 0,

    @ColumnInfo(name = FIELD_SNIPPET)
    val snippet: String,

    @ColumnInfo(name = FIELD_TEXT)
    var text: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    var updatedAt: Long,


    @ColumnInfo(name = FIELD_COORDINATES)
    val coordinates: Coordinates?,
) {

    companion object {
        const val TABLE_NAME = "pdf_comments"

        // Fields
        const val FIELD_ID = "id"
        const val FIELD_PDF_ID = "pdf_id"
        const val FIELD_SNIPPET = "snippet"
        const val FIELD_TEXT = "text"
        const val FIELD_PAGE = "page"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_COORDINATES = "coordinates"
    }
}

@Entity(tableName = HighlightEntity.TABLE_NAME)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long? = null,

    @ColumnInfo(name = FIELD_PDF_ID)
    val pdfId: Long = 0,

    @ColumnInfo(name = FIELD_SNIPPET)
    val snippet: String,

    @ColumnInfo(name = FIELD_COLOR)
    val color: String,

    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    val updatedAt: Long,

    @ColumnInfo(name = FIELD_COORDINATES)
    val coordinates: Coordinates?,
) {

    companion object {

        const val TABLE_NAME = "pdf_highlights"

        // Fields
        const val FIELD_ID = "id"
        const val FIELD_PDF_ID = "pdf_id"
        const val FIELD_SNIPPET = "snippet"
        const val FIELD_COLOR = "color"
        const val FIELD_PAGE = "page"
        const val FIELD_UPDATED_AT = "updated_at"
        const val FIELD_COORDINATES = "coordinates"

    }
}

@Entity(tableName = BookmarkEntity.TABLE_NAME)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FIELD_ID)
    val id: Long? = null,

    @ColumnInfo(name = FIELD_PDF_ID)
    val pdfId: Long = 0,


    @ColumnInfo(name = FIELD_PAGE)
    val page: Int,

    @ColumnInfo(name = FIELD_UPDATED_AT)
    val updatedAt: Long,

) {


    companion object {
        const val TABLE_NAME = "pdfBookmarks"

        // Fields
        const val FIELD_ID = "id"
        const val FIELD_PDF_ID = "pdf_id"
        const val FIELD_PAGE = "page"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}
