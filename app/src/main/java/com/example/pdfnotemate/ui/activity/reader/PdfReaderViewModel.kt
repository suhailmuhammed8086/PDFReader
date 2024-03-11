package com.example.pdfnotemate.ui.activity.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfnotemate.data.ValidationErrorException
import com.example.pdfnotemate.model.AnnotationListResponse
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.tools.OperationsStateHandler
import com.example.pdfnotemate.tools.pdf.viewer.model.Coordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
): ViewModel(){

    var pdfDetails: PdfNoteListModel? = null
    var annotations = AnnotationListResponse()

    val addCommentResponse = OperationsStateHandler(viewModelScope)
    val addHighlightResponse = OperationsStateHandler(viewModelScope)
    val addBookmarkResponse = OperationsStateHandler(viewModelScope)
    val deleteBookmarkResponse = OperationsStateHandler(viewModelScope)
    val annotationListResponse = OperationsStateHandler(viewModelScope)


    fun loadAllAnnotations() {
        if (pdfDetails == null) return
        annotationListResponse.load {
            pdfRepository.getAllAnnotations(pdfDetails!!.id)
        }
    }
    fun addComment(snippet: String, text: String, page: Int, coordinates: Coordinates?) {
        if (pdfDetails == null) return
        addCommentResponse.load {
            if (coordinates == null) throw ValidationErrorException(1,"Coordinates not found")
            pdfRepository.addComment(
                pdfDetails!!.id,
                snippet,
                text,
                page,
                coordinates
            )
        }
    }
    fun addHighlight(snippet: String, color: String, page: Int, coordinates: Coordinates) {
        if (pdfDetails == null) return
        addHighlightResponse.load {
            pdfRepository.addHighlight(
                pdfDetails!!.id,
                snippet,
                color,
                page,
                coordinates
            )
        }
    }
    fun addBookmark(page: Int) {
        if (pdfDetails == null) return
        addBookmarkResponse.load {
            pdfRepository.addBookmark(
                pdfDetails!!.id,
                page,
            )
        }
    }
    fun removeBookmark(page: Int) {
        if (pdfDetails == null) return
        deleteBookmarkResponse.load {
            pdfRepository.deleteBookmarkWithPageAndPdfId(
                page,
                pdfDetails!!.id,
            )
        }
    }


}