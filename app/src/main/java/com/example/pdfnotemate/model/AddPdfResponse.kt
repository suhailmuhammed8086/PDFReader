package com.example.pdfnotemate.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class PdfNotesResponse(
    val notes : List<PdfNoteListModel>
)

@Parcelize
data class PdfNoteListModel(
    val id: Long,
    val title: String,
    val tag: TagModel?,
    val about: String?,
    val updatedTime: Long,
    val highlights: Int,
    val comments: Int,
    val bookmarks:Int
): Parcelable

@Parcelize
data class TagModel(
    val id: Long,
    val title: String,
    val colorCode: String?
): Parcelable