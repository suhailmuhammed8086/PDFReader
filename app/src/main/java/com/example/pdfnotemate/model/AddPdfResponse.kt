package com.example.pdfnotemate.model


data class PdfNotesResponse(
    val notes : List<PdfNoteListModel>
)

data class PdfNoteListModel(
    val id: Long,
    val title: String,
    val tag: TagModel?,
    val about: String?,
    val updatedTime: Long
)

data class TagModel(
    val id: Long,
    val title: String,
    val colorCode: String?
)