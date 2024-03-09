package com.example.pdfnotemate.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(PdfTagEntity.TABLE_NAME)
data class PdfTagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(FIELD_ID)
    val id: Long? = null,
    @ColumnInfo(FIELD_TITLE)
    val title: String,
    @ColumnInfo(FIELD_COLOR_CODE)
    val colorCode: String?
) {
    companion object {
        const val TABLE_NAME = "PdfTagEntity"

        const val FIELD_ID = "id"
        const val FIELD_TITLE = "title"
        const val FIELD_COLOR_CODE = "colorCode"
    }
}
