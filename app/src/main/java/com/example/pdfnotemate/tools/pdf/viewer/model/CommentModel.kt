package com.example.pdfnotemate.tools.pdf.viewer.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentModel(
    val id: Long,
    val snippet: String,
    var text: String,
    val page: Int,
    var updatedAt: Long,
    val coordinates: Coordinates?,
) : PdfAnnotationModel(Type.Note, page - 1), Parcelable {
    @IgnoredOnParcel
    var isSelected = false

    /**This function will update tha value in the parent Annotation class, like pagination page index and annotation type*/
    fun updateAnnotationData(): CommentModel {
        super.paginationPageIndex = page - 1
        super.type = Type.Note
        return this
    }
}

@Parcelize
data class HighlightModel(
    val id: Long,
    val snippet: String,
    val color: String,
    val page: Int,
    val updatedAt: Long,
    val coordinates: Coordinates?,
) : PdfAnnotationModel(Type.Highlight, page - 1), Parcelable {
    @IgnoredOnParcel
    var isSelected = false


    /**This function will update tha value in the parent Annotation class, like pagination page index and annotation type*/
    fun updateAnnotationData(): HighlightModel {
        super.paginationPageIndex = page - 1
        super.type = Type.Highlight
        return this
    }
}

@Parcelize
data class BookmarkModel(
    val id: Long,
    val page: Int,
    val updatedAt: Long,
) : Parcelable {
    @IgnoredOnParcel
    var isSelected = false
}



@Parcelize
data class Coordinates(
    var startX: Double,
    var startY: Double,
    var endX: Double,
    var endY: Double,
) : Parcelable
