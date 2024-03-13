package com.example.pdfnotemate.ui.activity.annotations.highlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.tools.OperationsStateHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HighlightListViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
) : ViewModel() {
    val deleteHighlightResponse = OperationsStateHandler(viewModelScope)

    fun deleteHighlights(ids: List<Long>) {
        deleteHighlightResponse.load {
            pdfRepository.deleteHighlight(ids)
        }
    }
}