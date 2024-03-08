package com.example.pdfnotemate.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(PdfNoteEntity.TABLE_NAME)
data class PdfNoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(FIELD_ID)
    val id: Long? = null,

    @ColumnInfo(FIELD_TITLE)
    val title: String,
    @ColumnInfo(FIELD_FILE_PATH)
    val filePath: String,
    @ColumnInfo(FIELD_ABOUT)
    val about: String?,
    @ColumnInfo(FIELD_TAG_ID)
    val tagId: Int,
    @ColumnInfo(FIELD_UPDATED_AT)
    val updateAt: String,
) {


    companion object {
        const val TABLE_NAME = "PdfNoteEntity"

        const val FIELD_ID = "id"
        const val FIELD_TITLE = "title"
        const val FIELD_FILE_PATH = "file_path"
        const val FIELD_ABOUT = "about"
        const val FIELD_TAG_ID = "tagId"
        const val FIELD_UPDATED_AT = "updated_at"
    }
}