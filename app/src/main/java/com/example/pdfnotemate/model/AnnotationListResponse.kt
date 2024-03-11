package com.example.pdfnotemate.model

import com.example.pdfnotemate.tools.pdf.viewer.model.BookmarkModel
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel

data class AnnotationListResponse(
    val comments: ArrayList<CommentModel> = arrayListOf(),
    val highlights: ArrayList<HighlightModel> = arrayListOf(),
    val bookmarks: ArrayList<BookmarkModel> = arrayListOf()
)
